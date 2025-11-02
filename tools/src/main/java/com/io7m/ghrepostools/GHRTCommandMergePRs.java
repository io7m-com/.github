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

import com.io7m.ghrepostools.github_api.GHub;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.KeyIdentifier;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;

public final class GHRTCommandMergePRs implements QCommandType
{
  private final QCommandMetadata metadata;

  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandMergePRs.class);

  private static final QParameterNamed1<Path> AUDIT =
    new QParameterNamed1<>(
      "--audit-log",
      List.of(),
      new QStringType.QConstant(
        "The audit log."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Boolean> MERGE =
    new QParameterNamed1<>(
      "--merge",
      List.of(),
      new QStringType.QConstant(
        "Actually merge PRs instead of only showing what would be merged."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private static final QParameterNamed1<Pattern> REPOSITORIES_INCLUDE =
    new QParameterNamed1<>(
      "--repositories-include",
      List.of(),
      new QStringType.QConstant("The repositories to include."),
      Optional.empty(),
      Pattern.class
    );

  private static final QParameterNamed1<Pattern> REPOSITORIES_EXCLUDE =
    new QParameterNamed1<>(
      "--repositories-exclude",
      List.of(),
      new QStringType.QConstant("The repositories to exclude."),
      Optional.of(Pattern.compile("")),
      Pattern.class
    );

  private static final QParameterNamed1<Integer> SLEEP_BETWEEN =
    new QParameterNamed1<>(
      "--sleep-between",
      List.of(),
      new QStringType.QConstant("The number of seconds to sleep between PRs."),
      Optional.of(30),
      Integer.class
    );

  private Pattern reposInclude;
  private Pattern reposExclude;
  private boolean doMerge;
  private long sleepSeconds;
  private GHRTAuditLog audit;
  private BigInteger prsNotMergeable = BigInteger.ZERO;
  private BigInteger prsFailedMerges = BigInteger.ZERO;
  private BigInteger prsMerged = BigInteger.ZERO;
  private BigInteger prsSeen = BigInteger.ZERO;

  /**
   * Construct a command.
   */

  public GHRTCommandMergePRs()
  {
    this.metadata = new QCommandMetadata(
      "merge-prs",
      new QStringType.QConstant("Merge pull requests automatically."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(
      List.of(
        MERGE,
        REPOSITORIES_INCLUDE,
        REPOSITORIES_EXCLUDE,
        SLEEP_BETWEEN,
        AUDIT
      )
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    this.reposInclude =
      context.parameterValue(REPOSITORIES_INCLUDE);
    this.reposExclude =
      context.parameterValue(REPOSITORIES_EXCLUDE);
    this.doMerge =
      context.parameterValue(MERGE).booleanValue();
    this.sleepSeconds =
      context.parameterValue(SLEEP_BETWEEN).longValue();
    this.audit =
      GHRTAuditLog.open(context.parameterValue(AUDIT));

    try (final var ignored = this.audit) {
      try (final var gh = GHub.create()) {
        final var repositories =
          new ArrayList<>(
            gh.repositories(this.reposInclude, this.reposExclude)
          );
        Collections.shuffle(repositories);

        LOG.info("Processing {} repositories", repositories.size());
        for (final var repository : repositories) {
          try {
            MDC.put("Repository", repository.fullName());
            this.processRepository(gh, repository);
          } finally {
            MDC.remove("Repository");
          }
        }
      }
    }

    LOG.info("PRs seen:         {}", this.prsSeen);
    LOG.info("PRs merged:       {}", this.prsMerged);
    LOG.info("PRs failed merge: {}", this.prsFailedMerges);
    LOG.info("PRs not ready:    {}", this.prsNotMergeable);
    return QCommandStatus.SUCCESS;
  }

  private void processRepository(
    final GHub gh,
    final GHub.GRepository repository)
    throws Exception
  {
    LOG.info("Processing PRs");

    final var reposName = repository.fullName();
    final var prs = gh.pullRequests(reposName);
    for (final var prSrc : prs) {
      this.prsSeen = this.prsSeen.add(BigInteger.ONE);

      final var pr = gh.pullRequestFull(reposName, prSrc.number());
      MDC.put("PR", pr.number().toString());

      if (this.isPullRequestReady(gh, reposName, pr)) {
        LOG.info("Passed all merge criteria");
        if (this.doMerge) {
          LOG.info("Merging");

          try {
            final var auditId = UUID.randomUUID();
            gh.pullRequestMerge(reposName, auditId, pr);

            LOG.info("Merged");
            this.audit.write(new GHRTAuditLog.AuditEvent(
              "Merged",
              auditId,
              OffsetDateTime.now(UTC),
              Map.ofEntries(
                Map.entry("Repository", reposName),
                Map.entry("PR", pr.number().toString()),
                Map.entry("PRTitle", pr.title())
              )
            ));

            this.prsMerged = this.prsMerged.add(BigInteger.ONE);
            LOG.info("Sleeping {} seconds", this.sleepSeconds);
            TimeUnit.SECONDS.sleep(this.sleepSeconds);
          } catch (final Exception e) {
            LOG.error("Failed to merge: ", e);
            this.prsFailedMerges = this.prsFailedMerges.add(BigInteger.ONE);
          }
        }
      } else {
        LOG.warn("Failed one or more merge criteria");
        this.prsNotMergeable = this.prsNotMergeable.add(BigInteger.ONE);
      }
    }
  }

  private boolean isPullRequestReady(
    final GHub gh,
    final String reposName,
    final GHub.GPullRequest pr)
    throws Exception
  {
    LOG.info("Checking '{}'", pr.title());
    var ok = true;
    ok = ok & this.isPullRequestReadyMergeable(gh, pr);
    ok = ok & this.isPullRequestReadyCommits(gh, pr);
    ok = ok & this.isPullRequestWorkflowsDone(gh, reposName, pr);
    return ok;
  }

  private boolean isPullRequestReadyMergeable(
    final GHub gh,
    final GHub.GPullRequest pr)
  {
    if (!pr.mergeable()) {
      LOG.warn("Not mergeable");
      return false;
    }

    LOG.info("Mergeable");
    return true;
  }

  /**
   * Check that all workflows for the last commit in the PR have run
   * successfully.
   */

  private boolean isPullRequestWorkflowsDone(
    final GHub gh,
    final String reposName,
    final GHub.GPullRequest pr)
    throws Exception
  {
    final var commits =
      gh.pullRequestCommits(pr);
    final var last =
      commits.getLast();

    MDC.put("Commit", last.sha());

    try {
      final var workflowRuns =
        gh.pullRequestWorkflowRuns(reposName, pr, last)
          .workflowRuns()
          .stream()
          .collect(Collectors.toMap(GHub.GWorkflowRun::name, e -> e));

      final var requiredWorkflows =
        new GHRTWorkflows().workflows();

      for (final var requiredWorkflow : requiredWorkflows) {
        final var name = requiredWorkflow.prName();
        final var workflowRun = workflowRuns.get(name);
        if (workflowRun == null) {
          LOG.warn("No workflow run for {}", name);
          return false;
        }

        final var succeeded =
          Objects.equals(workflowRun.conclusion(), "success");
        final var completed =
          Objects.equals(workflowRun.status(), "completed");

        if (succeeded && completed) {
          LOG.info(
            "Required workflow '{}' has conclusion '{}' and status '{}'",
            name,
            workflowRun.conclusion(),
            workflowRun.status()
          );
        } else {
          LOG.warn(
            "Required workflow '{}' has conclusion '{}' and status '{}'",
            name,
            workflowRun.conclusion(),
            workflowRun.status()
          );
          return false;
        }
      }

      LOG.info("All required workflows have succeeded");
      return true;
    } finally {
      MDC.remove("Commit");
    }
  }

  private boolean isPullRequestReadyCommits(
    final GHub gh,
    final GHub.GPullRequest pr)
    throws Exception
  {
    final var commits = gh.pullRequestCommits(pr);
    var ok = true;
    for (final var commit : commits) {
      ok = ok & this.isPullRequestReadyCommitUserAllowed(pr, commit);
      ok = ok & this.isPullRequestReadyCommitSignatureVerified(pr, commit);
      ok = ok & this.isPullRequestReadyCommitSignatureKeys(pr, commit);
    }
    return ok;
  }

  /**
   * Check that all commits within the PR are signed with keys that we
   * recognize.
   */

  private boolean isPullRequestReadyCommitSignatureKeys(
    final GHub.GPullRequest pr,
    final GHub.GCommit commit)
    throws IOException
  {
    MDC.put("Commit", commit.sha());

    try {
      final var rawCommit =
        commit.commit();
      final var verification =
        rawCommit.verification();
      final var sigText =
        verification.signature();
      final var stream =
        new ArmoredInputStream(
          new ByteArrayInputStream(sigText.getBytes(StandardCharsets.UTF_8))
        );
      final var factory =
        new JcaPGPObjectFactory(stream);

      var sigKeysAllowed = 0;
      var sigKeysChecked = 0;

      for (final var object : factory) {
        switch (object) {
          case final PGPSignatureList sigList -> {
            for (final var sig : sigList) {
              for (final var keyId : sig.getKeyIdentifiers()) {
                ++sigKeysChecked;
                sigKeysAllowed += checkSignatureHasAllowedKey(
                  pr,
                  commit,
                  keyId);
              }
            }
          }
          case final PGPSignature sig -> {
            for (final var keyId : sig.getKeyIdentifiers()) {
              ++sigKeysChecked;
              sigKeysAllowed += checkSignatureHasAllowedKey(pr, commit, keyId);
            }
          }
          default -> {

          }
        }
      }

      if (sigKeysChecked > 0 && sigKeysChecked == sigKeysAllowed) {
        LOG.info("Commit has signature(s) from allowed keys");
        return true;
      }

      LOG.warn("Commit has signature(s) from unrecognized keys");
      return false;
    } finally {
      MDC.remove("Commit");
    }
  }

  private static int checkSignatureHasAllowedKey(
    final GHub.GPullRequest pr,
    final GHub.GCommit commit,
    final KeyIdentifier keyId)
  {
    MDC.put("Commit", commit.sha());

    try {
      for (final var key : GHRTKeys.allowedPGPKeys()) {
        if (keyId.getKeyId() == key.id()) {
          LOG.info(
            "Signed by allowed key 0x{} ({})",
            Long.toUnsignedString(keyId.getKeyId(), 16),
            key.name()
          );
          return 1;
        }
      }

      LOG.warn(
        "Unrecognized key ID 0x{}",
        Long.toUnsignedString(keyId.getKeyId(), 16)
      );
      return 0;
    } finally {
      MDC.remove("Commit");
    }
  }

  /**
   * Check that GitHub thinks that the commit signature is verified.
   */

  private boolean isPullRequestReadyCommitSignatureVerified(
    final GHub.GPullRequest pr,
    final GHub.GCommit commit)
  {
    MDC.put("Commit", commit.sha());

    try {
      final var rawCommit = commit.commit();
      final var verification = rawCommit.verification();
      if (!verification.verified()) {
        LOG.warn("Commit is not verified");
        return false;
      }

      LOG.info("Commit is verified");
      return true;
    } finally {
      MDC.remove("Commit");
    }
  }

  /**
   * Check that the given commit comes from a user we recognize.
   */

  private boolean isPullRequestReadyCommitUserAllowed(
    final GHub.GPullRequest pr,
    final GHub.GCommit commit)
  {
    MDC.put("Commit", commit.sha());

    try {
      final var author =
        commit.author();
      final var authorId =
        author.id().longValueExact();
      final var users =
        GHRTUsers.allowedPRMergeUsers();

      for (final var user : users) {
        final var idOk = authorId == user.id();
        if (idOk) {
          LOG.info(
            "User id {} ({}) is in the allowed list",
            authorId,
            user.name()
          );
          return true;
        }
      }

      LOG.warn(
        "User id {} is not in the allowed list",
        authorId
      );
      return false;
    } finally {
      MDC.remove("Commit");
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
