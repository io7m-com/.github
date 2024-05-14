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

import com.io7m.ghrepostools.templating.GHRTTemplateService;
import com.io7m.ghrepostools.templating.GHRTWorkflowModel;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.ghrepostools.GHRTExecutable.EXECUTABLE;
import static com.io7m.ghrepostools.GHRTExecutable.NOT_EXECUTABLE;
import static com.io7m.ghrepostools.GHRTVideoRecordingEnabled.VIDEO_RECORDING_DISABLED;
import static com.io7m.ghrepostools.GHRTVideoRecordingEnabled.VIDEO_RECORDING_ENABLED;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class GHRTCommandWorkflows implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandWorkflows.class);
  public static final OpenOption[] FILE_WRITE_OPTIONS = {
    WRITE, TRUNCATE_EXISTING, CREATE
  };

  private final QCommandMetadata metadata;
  private Path workflowDirectory;
  private Path githubDirectory;

  /**
   * Construct a command.
   */

  public GHRTCommandWorkflows()
  {
    this.metadata =
      new QCommandMetadata(
        "workflows",
        new QStringType.QConstant("Generate workflow files."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of();
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    this.githubDirectory =
      Path.of("");
    this.githubDirectory =
      this.githubDirectory.resolve(".github");
    this.workflowDirectory =
      this.githubDirectory.resolve("workflows");

    final var templates =
      GHRTTemplateService.create();

    final Optional<GHRTWorkflowProfile> workflowProfileOpt =
      this.findWorkflowProfile();

    if (workflowProfileOpt.isEmpty()) {
      return QCommandStatus.SUCCESS;
    }

    final GHRTWorkflowProfile workflowProfile =
      workflowProfileOpt.orElseThrow();

    final var names =
      GHRTProjectNames.projectName();

    {
      Files.createDirectories(this.workflowDirectory);

      final var workflows =
        new GHRTWorkflows()
          .workflows();

      for (final var workflow : workflows) {
        final var mainFile =
          this.workflowDirectory.resolve(
            "%s.yml".formatted(workflow.mainName()));
        final var prFile =
          this.workflowDirectory.resolve(
            "%s.yml".formatted(workflow.prName()));

        LOG.info("writing {}", mainFile);
        LOG.info("writing {}", prFile);

        try (var output =
               Files.newBufferedWriter(mainFile, FILE_WRITE_OPTIONS)) {

          final var template =
            templates.workflowMain();

          final var videoRecordingEnabled =
            switch (workflowProfile.videoRecordingEnabled()) {
              case VIDEO_RECORDING_ENABLED ->
                switch (workflow.platform().videoSupported()) {
                  case VIDEO_SUPPORTED -> VIDEO_RECORDING_ENABLED;
                  case VIDEO_UNSUPPORTED -> VIDEO_RECORDING_DISABLED;
                };
              case VIDEO_RECORDING_DISABLED -> VIDEO_RECORDING_DISABLED;
            };

          final Optional<GHRTCustomRunScript> customRunScript =
            switch (workflowProfile.customRunScriptEnabled()) {
              case CUSTOM_RUN_SCRIPT_ENABLED ->
                Optional.of(workflow.platform().customRunScript());
              case CUSTOM_RUN_SCRIPT_DISABLED ->
                Optional.empty();
            };

          template.process(
            new GHRTWorkflowModel(
              GHRTActionVersions.get(),
              workflowProfile.name(),
              workflow.mainName(),
              workflow.platform().imageName(),
              Integer.toUnsignedString(workflow.jdkVersion()),
              workflow.jdkDistribution().lowerName(),
              names.shortName(),
              workflow.coverage(),
              workflow.deploy(),
              videoRecordingEnabled,
              customRunScript,
              "Push"
            ),
            output
          );

          output.newLine();
          output.flush();
        }

        try (var output =
               Files.newBufferedWriter(prFile, FILE_WRITE_OPTIONS)) {

          final var template =
            templates.workflowMain();

          final var videoRecordingEnabled =
            switch (workflowProfile.videoRecordingEnabled()) {
              case VIDEO_RECORDING_ENABLED ->
                switch (workflow.platform().videoSupported()) {
                  case VIDEO_SUPPORTED -> VIDEO_RECORDING_ENABLED;
                  case VIDEO_UNSUPPORTED -> VIDEO_RECORDING_DISABLED;
                };
              case VIDEO_RECORDING_DISABLED -> VIDEO_RECORDING_DISABLED;
            };

          final Optional<GHRTCustomRunScript> customRunScript =
            switch (workflowProfile.customRunScriptEnabled()) {
              case CUSTOM_RUN_SCRIPT_ENABLED ->
                Optional.of(workflow.platform().customRunScript());
              case CUSTOM_RUN_SCRIPT_DISABLED ->
                Optional.empty();
            };

          template.process(
            new GHRTWorkflowModel(
              GHRTActionVersions.get(),
              workflowProfile.name(),
              workflow.prName(),
              workflow.platform().imageName(),
              Integer.toUnsignedString(workflow.jdkVersion()),
              workflow.jdkDistribution().lowerName(),
              names.shortName(),
              GHRTCoverageEnabled.COVERAGE_DISABLED,
              GHRTDeployEnabled.DEPLOY_DISABLED,
              videoRecordingEnabled,
              customRunScript,
              "PullRequest"
            ),
            output
          );

          output.newLine();
          output.flush();
        }
      }
    }

    {
      final var filePath =
        this.workflowDirectory.resolve("deploy.linux.temurin.lts.yml");

      LOG.info("writing {}", filePath);

      try (var output =
             Files.newBufferedWriter(filePath, FILE_WRITE_OPTIONS)) {

        final var template =
          templates.deployMain();

        template.process(
          new GHRTWorkflowModel(
            GHRTActionVersions.get(),
            GHRTWorkflowProfile.core().name(),
            "deploy.linux.temurin.lts",
            GHRTPlatform.LINUX.imageName(),
            Integer.toUnsignedString(GHRTWorkflows.JDK_LTS),
            GHRTJDKDistribution.TEMURIN.lowerName(),
            names.shortName(),
            GHRTCoverageEnabled.COVERAGE_ENABLED,
            GHRTDeployEnabled.DEPLOY_ENABLED,
            VIDEO_RECORDING_DISABLED,
            Optional.empty(),
            ""
          ),
          output
        );

        output.newLine();
        output.flush();
      }
    }

    this.writeFileResource("Tools.java", NOT_EXECUTABLE);
    this.writeFileResource("deploy-release.sh", EXECUTABLE);
    this.writeFileResource("deploy-snapshot.sh", EXECUTABLE);
    this.writeFileResource("run-with-xvfb.sh", EXECUTABLE);
    this.writeFileResource("wallpaper.png", NOT_EXECUTABLE);
    return QCommandStatus.SUCCESS;
  }

  private void writeFileResource(
    final String file,
    final GHRTExecutable executable)
    throws IOException
  {
    final var filePath =
      this.workflowDirectory.resolve(file);

    LOG.info("Writing {}", filePath);

    try (var out = Files.newOutputStream(filePath, FILE_WRITE_OPTIONS)) {
      try (var in = GHRTCommandWorkflows.class
        .getResourceAsStream("/com/io7m/ghrepostools/%s".formatted(file))) {
        in.transferTo(out);
        out.flush();
      }
    }

    switch (executable) {
      case EXECUTABLE -> {
        Files.setPosixFilePermissions(
          filePath,
          PosixFilePermissions.fromString("rwxr-xr-x")
        );
      }
      case NOT_EXECUTABLE -> {
        Files.setPosixFilePermissions(
          filePath,
          PosixFilePermissions.fromString("rw-r--r--")
        );
      }
    }
  }

  private Optional<GHRTWorkflowProfile> findWorkflowProfile()
    throws IOException
  {
    final var path =
      this.githubDirectory.resolve("workflows.conf");

    if (!Files.exists(path)) {
      return Optional.of(GHRTWorkflowProfile.core());
    }

    final var props = new Properties();
    try (var stream = Files.newInputStream(path)) {
      props.load(stream);
    }
    final var profileName =
      props.getProperty("ProfileName").trim();

    if ("None".equals(profileName)) {
      return Optional.empty();
    }

    final var profiles =
      GHRTWorkflowProfile.profiles();

    final var workflowProfile = profiles.get(profileName);
    if (workflowProfile == null) {
      throw new IllegalStateException(
        "Unrecognized profile name: %s".formatted(profileName)
      );
    }
    return Optional.of(workflowProfile);
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
