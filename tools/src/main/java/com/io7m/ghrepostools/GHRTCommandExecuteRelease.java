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

import com.io7m.changelog.core.CChangelog;
import com.io7m.changelog.core.CChangelogFilters;
import com.io7m.changelog.core.CRelease;
import com.io7m.changelog.core.CVersion;
import com.io7m.changelog.core.CVersions;
import com.io7m.changelog.text.api.CPlainChangelogWriterConfiguration;
import com.io7m.changelog.text.vanilla.CPlainChangelogWriters;
import com.io7m.changelog.xml.CXMLChangelogParsers;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class GHRTCommandExecuteRelease implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandExecuteRelease.class);

  private static final CXMLChangelogParsers PARSERS =
    new CXMLChangelogParsers();
  private static final CPlainChangelogWriters WRITERS =
    new CPlainChangelogWriters();

  private static final OpenOption[] OPTIONS_REPLACE = {
    WRITE, TRUNCATE_EXISTING, CREATE
  };

  private final QCommandMetadata metadata;

  private static final QParameterNamed1<String> VERSION_PREVIOUS =
    new QParameterNamed1<>(
      "--version-previous",
      List.of(),
      new QStringType.QConstant("The previous release version."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> VERSION_NEXT =
    new QParameterNamed1<>(
      "--version-next",
      List.of(),
      new QStringType.QConstant("The next expected release version."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> VERSION =
    new QParameterNamed1<>(
      "--version",
      List.of(),
      new QStringType.QConstant("The release version."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> PROJECT =
    new QParameterNamed1<>(
      "--project",
      List.of(),
      new QStringType.QConstant("The project name."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> URI_BASE =
    new QParameterNamed1<>(
      "--uri-base",
      List.of(),
      new QStringType.QConstant("The URI base."),
      Optional.of("https://www.github.com/io7m-com/"),
      String.class
    );

  private static final QParameterNamed1<String> MASTER_BRANCH =
    new QParameterNamed1<>(
      "--master-branch",
      List.of(),
      new QStringType.QConstant("The master branch name."),
      Optional.empty(),
      String.class
    );

  /**
   * Construct a command.
   */

  public GHRTCommandExecuteRelease()
  {
    this.metadata =
      new QCommandMetadata(
        "execute-release",
        new QStringType.QConstant("Execute a full project release."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      MASTER_BRANCH,
      PROJECT,
      URI_BASE,
      VERSION,
      VERSION_PREVIOUS,
      VERSION_NEXT
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var project =
      context.parameterValue(PROJECT);
    final var versionPrev =
      context.parameterValue(VERSION_PREVIOUS);
    final var versionNext =
      context.parameterValue(VERSION_NEXT);
    final var version =
      context.parameterValue(VERSION);
    final var uriBase =
      context.parameterValue(URI_BASE);
    final var masterBranch =
      context.parameterValue(MASTER_BRANCH);

    final var projectDirectory =
      Paths.get(project)
        .toAbsolutePath();
    final var siteDirectory =
      Paths.get(project + "-SITE")
        .toAbsolutePath();

    if (Files.isDirectory(projectDirectory)) {
      throw new IllegalStateException(
        "A directory %s already exists".formatted(projectDirectory)
      );
    }
    if (Files.isDirectory(siteDirectory)) {
      throw new IllegalStateException(
        "A directory %s already exists".formatted(siteDirectory)
      );
    }

    Files.createDirectories(siteDirectory);

    final var releaseTag =
      String.format("com.io7m.%s-%s", project, version);

    LOG.info("Release tag will be: {}", releaseTag);

    final var cloneURI =
      URI.create("%s/%s".formatted(uriBase, project))
        .normalize();

    LOG.info("Clone {}", cloneURI);

    executeProgram(
      Paths.get("."),
      "git",
      "clone",
      "--recursive",
      cloneURI.toString(),
      project
    );

    final var releaseBranch =
      "release/%s".formatted(version);

    executeProgram(
      projectDirectory,
      "git",
      "checkout",
      "-b",
      releaseBranch
    );

    LOG.info("Setting up release metadata...");

    final var pomFile =
      projectDirectory.resolve("pom.xml");
    final var pomFileTemp =
      projectDirectory.resolve("pom.xml.tmp");

    rewritePreviousVersion(
      pomFile,
      pomFileTemp,
      versionPrev
    );

    executeProgram(
      projectDirectory,
      "mvn",
      "versions:set",
      "-DnewVersion=%s".formatted(version)
    );

    final var cVersion =
      setupChangelog(projectDirectory, version);

    LOG.info("Updating changelog...");

    executeProgram(
      projectDirectory,
      "ghtools",
      "update-changelog",
      "--write",
      "true"
    );

    executeProgram(
      projectDirectory,
      "changelog",
      "release-finish"
    );

    final var releaseMessage =
      createReleaseMessage(projectDirectory, cVersion);

    final var mergeMasterMessage =
      "Merge branch '%s'\n\n%s".formatted(releaseBranch, releaseMessage);

    final var fileCommitRelease =
      Paths.get(project + "-commit-release.txt")
        .toAbsolutePath();

    Files.writeString(fileCommitRelease, releaseMessage, OPTIONS_REPLACE);

    final var fileMergeMaster =
      Paths.get(project + "-commit-master-merge.txt")
        .toAbsolutePath();

    Files.writeString(fileMergeMaster, mergeMasterMessage, OPTIONS_REPLACE);

    LOG.info("Committing release changes...");

    executeProgram(
      projectDirectory,
      "git",
      "commit",
      "-a",
      "-m",
      "Mark release %s".formatted(version)
    );

    LOG.info("Executing build...");

    executeProgram(
      projectDirectory,
      "mvn",
      "-Dgpg.skip",
      "-DskipTests=true",
      "-DskipITs=true",
      "-Denforcer.skip=true",
      "clean",
      "verify",
      "site"
    );

    LOG.info("Merging release branch into master branch...");

    executeProgram(
      projectDirectory,
      "git",
      "checkout",
      masterBranch
    );

    executeProgram(
      projectDirectory,
      "git",
      "merge",
      "--no-ff",
      releaseBranch,
      "-F",
      fileCommitRelease.toString()
    );

    executeProgram(
      projectDirectory,
      "git",
      "tag",
      "-s",
      "-F",
      fileCommitRelease.toString(),
      releaseTag
    );

    LOG.info("Merging master branch into develop branch...");

    executeProgram(
      projectDirectory,
      "git",
      "checkout",
      "develop"
    );

    executeProgram(
      projectDirectory,
      "git",
      "merge",
      "--no-ff",
      masterBranch,
      "-F",
      fileMergeMaster.toString()
    );

    LOG.info("Starting next version...");

    rewritePreviousVersion(pomFile, pomFileTemp, version);

    executeProgram(
      projectDirectory,
      "mvn",
      "versions:set",
      "-DnewVersion=%s-SNAPSHOT".formatted(versionNext)
    );

    executeProgram(
      projectDirectory,
      "git",
      "commit",
      "-a",
      "-m",
      "Begin next development iteration."
    );

    executeProgram(
      projectDirectory,
      "git",
      "branch",
      "-d",
      releaseBranch
    );

    LOG.info("Copying site data...");

    executeProgram(
      Paths.get("."),
      "rsync",
      "-av",
      projectDirectory.resolve("target").resolve("minisite") + "/",
      siteDirectory + "/"
    );

    LOG.info("You should now: ");
    LOG.info("  cd {} && git push --tags", projectDirectory);
    LOG.info("  cd {} && git push --all", projectDirectory);
    LOG.info(
      "  cd {} && ghtools push-io7m-site --project {}",
      projectDirectory.getParent(),
      project
    );
    return QCommandStatus.SUCCESS;
  }

  private static String createReleaseMessage(
    final Path projectDirectory,
    final CVersion version)
    throws IOException
  {
    final var file =
      projectDirectory.resolve("README-CHANGES.xml");
    final var changelog =
      getChangelog(file);

    final var configuration =
      CPlainChangelogWriterConfiguration.builder()
        .setShowDates(false)
        .build();

    final var changelogFiltered =
      CChangelogFilters.upToAndIncluding(changelog, version, 1)
        .orElseThrow(() -> {
          return new IllegalStateException("Could not filter changelog.");
        });

    final var output =
      new ByteArrayOutputStream();

    final var writer =
      WRITERS.createWithConfiguration(
        configuration,
        URI.create("urn:stdout"),
        output
      );

    writer.write(changelogFiltered);
    output.flush();
    return output.toString(StandardCharsets.UTF_8);
  }

  private static CVersion setupChangelog(
    final Path projectDirectory,
    final String version)
    throws IOException, InterruptedException
  {
    LOG.info("Setting up changelog...");

    final var openVersionOpt =
      getChangelogOpenRelease(
        projectDirectory.resolve("README-CHANGES.xml"));

    if (openVersionOpt.isPresent()) {
      final var openVersion =
        openVersionOpt.get();
      final var versionText =
        "%s".formatted(openVersion);

      if (!Objects.equals(versionText, version)) {
        throw new IllegalStateException(
          "The changelog has an open release %s, but we're trying to release %s"
            .formatted(versionText, version)
        );
      }

      LOG.info("Changelog has an open release {}, continuing...", versionText);
      return openVersion;
    }

    LOG.info("Opening a new changelog release...");
    executeProgram(
      projectDirectory,
      "changelog",
      "release-begin",
      "--version",
      version
    );
    return CVersions.parse(version);
  }

  private static CChangelog getChangelog(
    final Path file)
    throws IOException
  {
    try (var input = Files.newInputStream(file)) {
      final var parser =
        PARSERS.create(file.toUri(), input, error -> {
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

  private static Optional<CVersion> getChangelogOpenRelease(
    final Path file)
    throws IOException
  {
    return getChangelog(file)
      .findTargetReleaseOrLatestOpen(Optional.empty())
      .map(CRelease::version);
  }

  private static final Pattern PREVIOUS_VERSION_PATTERN =
    Pattern.compile(
      "^( +)<io7m.api.previousVersion>(.*)</io7m.api.previousVersion>$"
    );

  private static void rewritePreviousVersion(
    final Path pom,
    final Path pomTmp,
    final String version)
    throws IOException
  {
    final var lines =
      new ArrayList<>(Files.readAllLines(pom, StandardCharsets.UTF_8));

    var matched = false;
    for (int index = 0; index < lines.size(); ++index) {
      final var line =
        lines.get(index);
      final var matcher =
        PREVIOUS_VERSION_PATTERN.matcher(line);

      if (matcher.matches()) {
        final var indent =
          matcher.group(1);
        final var replacement =
          "%s<io7m.api.previousVersion>%s</io7m.api.previousVersion>"
            .formatted(indent, version);

        LOG.info("Replaced io7m.api.previousVersion");
        final var textNew = matcher.replaceFirst(replacement);
        lines.set(index, textNew);
        matched = true;
      }
    }

    if (!matched) {
      throw new IllegalStateException("No io7m.api.previousVersion in pom.xml");
    }

    Files.writeString(
      pomTmp,
      String.join("\n", lines),
      StandardCharsets.UTF_8,
      OPTIONS_REPLACE
    );

    Files.move(
      pomTmp,
      pom,
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING
    );
  }

  private static void executeProgram(
    final Path directory,
    final String... args)
    throws IOException, InterruptedException
  {
    LOG.debug("Execute {}", List.of(args));

    final var process =
      new ProcessBuilder(args)
        .directory(directory.toFile())
        .inheritIO()
        .start();

    process.waitFor();

    final var exitCode = process.exitValue();
    if (exitCode != 0) {
      throw new IOException(
        "Process returned exit code %d".formatted(Integer.valueOf(exitCode))
      );
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
