/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.ghrepostools;

import com.io7m.changelog.core.CChange;
import com.io7m.changelog.core.CChangelog;
import com.io7m.changelog.core.CRelease;
import com.io7m.changelog.core.CVersion;
import com.io7m.changelog.xml.CXMLChangelogParsers;
import com.io7m.changelog.xml.CXMLChangelogWriters;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;


public final class GHRTCommandUpdateChangelog implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandUpdateChangelog.class);

  private static final CXMLChangelogParsers PARSERS =
    new CXMLChangelogParsers();
  private static final CXMLChangelogWriters SERIALIZERS =
    new CXMLChangelogWriters();

  private final QCommandMetadata metadata;

  private static final QParameterNamed1<Boolean> UPDATE_CHANGELOG =
    new QParameterNamed1<>(
      "--write",
      List.of(),
      new QStringType.QConstant("Update README-CHANGES.xml."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  /**
   * Construct a command.
   */

  public GHRTCommandUpdateChangelog()
  {
    this.metadata = new QCommandMetadata(
      "update-changelog",
      new QStringType.QConstant(
        "Examine the git history and adjust the current changelog."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(UPDATE_CHANGELOG);
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var file =
      Paths.get("README-CHANGES.xml");
    final var fileTmp =
      Paths.get("README-CHANGES.xml.tmp");

    /*
     * Find the current (open) release, and the release that preceded it.
     */

    final CChangelog changelog =
      loadChangelog(file);

    final var releaseNow =
      changelog.findTargetReleaseOrLatestOpen(Optional.empty())
        .orElseThrow(() -> {
          return new IllegalStateException("No release is currently open!");
        });

    final var releaseThen =
      findReleasePrevious(changelog, releaseNow.version());

    LOG.info("Release current:  {}", releaseNow.version());
    LOG.info("Release previous: {}", releaseThen.version());

    /*
     * Find the current commit, and the commit that completed the previous
     * release. All the commits between the current commit and the last
     * release commit will be inspected for changelog entries.
     */

    final List<CChange> newChanges = new ArrayList<>();
    try (var git = Git.open(new File(".git"))) {
      final RevCommit commitNow = findCommitNow(git);
      LOG.info("Commit current: {}", commitNow);
      final RevCommit commitThen = findCommitThen(git, commitNow);
      LOG.info("Commit previous: {}", commitThen);
      generateChanges(git, releaseNow, commitThen, commitNow, newChanges);
    }

    /*
     * Add all the changes to the current release and save the changelog.
     */

    final var allChanges = new ArrayList<>(releaseNow.changes());
    allChanges.addAll(newChanges);
    allChanges.sort(Comparator.comparing(CChange::date));

    final var newRelease =
      CRelease.builder()
        .from(releaseNow)
        .setChanges(allChanges)
        .build();

    final var releasesUpdated = new TreeMap<>(changelog.releases());
    releasesUpdated.put(newRelease.version(), newRelease);

    final var newChangelog =
      CChangelog.builder()
        .from(changelog)
        .setReleases(releasesUpdated)
        .build();

    if (context.parameterValue(UPDATE_CHANGELOG).booleanValue()) {
      final var options = new OpenOption[] {
        WRITE, CREATE, TRUNCATE_EXISTING
      };

      try (var out = Files.newOutputStream(fileTmp, options)) {
        final var w =
          SERIALIZERS.create(fileTmp.toUri(), out);

        w.write(newChangelog);
        out.flush();
      }

      Files.move(
        fileTmp,
        file,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
      );
      LOG.info("Wrote {}", file);
    }

    return QCommandStatus.SUCCESS;
  }

  private static void generateChanges(
    final Git git,
    final CRelease releaseNow,
    final RevCommit commitThen,
    final RevCommit commitNow,
    final List<CChange> newChanges)
    throws Exception
  {
    final var iterator =
      git.log()
        .add(commitNow)
        .call()
        .iterator();

    while (iterator.hasNext()) {
      final var commit = iterator.next();
      LOG.debug("Inspecting commit: {}", commit);

      generateChange(git, releaseNow, commit, newChanges);
      if (commit.getId().equals(commitThen.getId())) {
        return;
      }
    }
  }

  private static void generateChange(
    final Git git,
    final CRelease releaseNow,
    final RevCommit commit,
    final List<CChange> newChanges)
  {
    if (isDependabot(commit)) {
      generateChangeForDependabot(git, releaseNow, commit, newChanges);
    }
  }

  private static final Pattern DEPENDABOT_BUMP_PATTERN_WITH_GROUP =
    Pattern.compile(
      "^bump (.*):(.*) from (.*) to (.*)$",
      Pattern.CASE_INSENSITIVE
    );

  private static final Pattern DEPENDABOT_BUMP_PATTERN_WITHOUT_GROUP =
    Pattern.compile(
      "^bump (.*) from (.*) to (.*)$",
      Pattern.CASE_INSENSITIVE
    );

  private static void generateChangeForDependabot(
    final Git git,
    final CRelease releaseNow,
    final RevCommit commit,
    final List<CChange> newChanges)
  {
    final var commitMessage = commit.getShortMessage();
    LOG.debug("Commit message: {}", commitMessage);

    final var matcherWithGroup =
      DEPENDABOT_BUMP_PATTERN_WITH_GROUP.matcher(commitMessage);
    final var matcherWithoutGroup =
      DEPENDABOT_BUMP_PATTERN_WITHOUT_GROUP.matcher(commitMessage);

    final String newMessage;
    if (matcherWithGroup.matches()) {
      final var group =
        matcherWithGroup.group(1);
      final var artifact =
        matcherWithGroup.group(2);
      final var versionFrom =
        matcherWithGroup.group(3);
      final var versionTo =
        matcherWithGroup.group(4);

      newMessage =
        "Update %s:%s:%s → %s.".formatted(
          group,
          artifact,
          versionFrom,
          versionTo
        );
    } else if (matcherWithoutGroup.matches()) {
      final var artifact =
        matcherWithoutGroup.group(1);
      final var versionFrom =
        matcherWithoutGroup.group(2);
      final var versionTo =
        matcherWithoutGroup.group(3);

      newMessage =
        "Update %s:%s → %s.".formatted(
          artifact,
          versionFrom,
          versionTo
        );
    } else {
      LOG.warn("Unparseable dependabot commit: {}", commitMessage);
      return;
    }

    final var existingChange =
      releaseNow.changes()
        .stream()
        .filter(c -> c.summary().trim().equals(newMessage))
        .findAny();

    if (existingChange.isPresent()) {
      LOG.info("Change already exists for message '{}'", newMessage);
      return;
    }

    final var time =
      commit.getCommitterIdent()
        .getWhenAsInstant();

    final var changeTime =
      ZonedDateTime.ofInstant(time, ZoneId.of("UTC"));

    LOG.info("Creating new change: {} {}", changeTime, newMessage);

    newChanges.add(
      CChange.builder()
        .setDate(changeTime)
        .setSummary(newMessage)
        .build()
    );
  }

  private static boolean isDependabot(
    final RevCommit commit)
  {
    return "dependabot[bot]".equals(commit.getAuthorIdent().getName());
  }

  private static RevCommit findCommitThen(
    final Git git,
    final RevCommit commitNow)
    throws Exception
  {
    RevCommit newestTagCommit = null;

    for (final var tag : git.tagList().call()) {
      LOG.info("Tag: {}", tag);

      final var obj =
        tag.getPeeledObjectId();

      if (obj == null) {
        continue;
      }

      final var log =
        git.log()
          .add(obj)
          .setMaxCount(1)
          .call();

      final var iterator = log.iterator();
      while (iterator.hasNext()) {
        final var commit = iterator.next();
        if (newestTagCommit == null) {
          newestTagCommit = commit;
          continue;
        }

        final var commitTime =
          commit.getCommitterIdent().getWhenAsInstant();
        final var existingTime =
          newestTagCommit.getCommitterIdent().getWhenAsInstant();

        if (commitTime.isAfter(existingTime)) {
          newestTagCommit = commit;
        }
      }
    }

    LOG.info("Commit at last tag: {}", newestTagCommit);
    if (newestTagCommit == null) {
      throw new IllegalStateException(
        "Could not locate a commit at the last tag!");
    }
    return newestTagCommit;
  }

  private static RevCommit findCommitNow(
    final Git git)
    throws GitAPIException
  {
    return git.log()
      .setMaxCount(1)
      .call()
      .iterator()
      .next();
  }

  private static CRelease findReleasePrevious(
    final CChangelog changelog,
    final CVersion releaseNow)
  {
    final var version =
      Optional.ofNullable(
        changelog.releases()
          .headMap(releaseNow)
          .lastKey()
      ).orElseThrow(() -> {
        return new IllegalStateException("No previous release exists!");
      });

    return changelog.releases().get(version);
  }


  private static CChangelog loadChangelog(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      final var parser =
        PARSERS.create(file.toUri(), stream, error -> {
          LOG.error(
            "{}: {}:{}: {}",
            error.severity(),
            Integer.valueOf(error.lexical().line()),
            Integer.valueOf(error.lexical().column()),
            error.message()
          );
        });
      return parser.parse();
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
