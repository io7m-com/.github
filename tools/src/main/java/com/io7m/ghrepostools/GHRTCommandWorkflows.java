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

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

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
    final var names =
      GHRTProjectNames.projectName();

    final var resources =
      GHRTStrings.ofXMLResource(
        GHRTCommandWorkflows.class,
        "/com/io7m/ghrepostools/Strings.xml"
      );

    {
      var path = Path.of("");
      path = path.resolve(".github");
      path = path.resolve("workflows-are-custom");

      if (Files.exists(path)) {
        return QCommandStatus.SUCCESS;
      }
    }

    var workflowDirectory = Path.of("");
    workflowDirectory = workflowDirectory.resolve(".github");
    workflowDirectory = workflowDirectory.resolve("workflows");

    {
      Files.createDirectories(workflowDirectory);

      final var workflows =
        new GHRTWorkflows()
          .workflows();

      final var templates =
        GHRTTemplateService.create();

      for (final var workflow : workflows) {
        final var mainFile =
          workflowDirectory.resolve("%s.yml".formatted(workflow.mainName()));
        final var prFile =
          workflowDirectory.resolve("%s.yml".formatted(workflow.prName()));

        LOG.info("writing {}", mainFile);
        LOG.info("writing {}", prFile);

        try (var output =
               Files.newBufferedWriter(mainFile, FILE_WRITE_OPTIONS)) {

          final var template =
            templates.workflowMain();

          template.process(
            new GHRTWorkflowModel(
              workflow.mainName(),
              workflow.platform().imageName(),
              Integer.toUnsignedString(workflow.jdkVersion()),
              workflow.jdkDistribution().lowerName(),
              names.shortName(),
              workflow.coverage(),
              workflow.deploy(),
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

          template.process(
            new GHRTWorkflowModel(
              workflow.prName(),
              workflow.platform().imageName(),
              Integer.toUnsignedString(workflow.jdkVersion()),
              workflow.jdkDistribution().lowerName(),
              names.shortName(),
              false,
              false,
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
        workflowDirectory.resolve("Tools.java");

      LOG.info("writing {}", filePath);

      try (var out = Files.newOutputStream(filePath, FILE_WRITE_OPTIONS)) {
        try (var in = GHRTCommandWorkflows.class
          .getResourceAsStream("/com/io7m/ghrepostools/embedded/Tools.java")) {
          in.transferTo(out);
          out.flush();
        }
      }
    }

    {
      final var filePath =
        workflowDirectory.resolve("deploy.linux.temurin.lts.yml");

      LOG.info("writing {}", filePath);

      try (var output =
             Files.newBufferedWriter(filePath, FILE_WRITE_OPTIONS)) {

        output.append(
          MessageFormat.format(
            resources.getString("workflowDeployTemplate"),
            "deploy.linux.temurin.lts",
            GHRTPlatform.LINUX.imageName(),
            Integer.valueOf(GHRTWorkflows.JDK_LTS),
            "'" + GHRTJDKDistribution.TEMURIN.lowerName() + "'",
            names.shortName()
          )
        );
      }
    }

    {
      final var filePath =
        workflowDirectory.resolve("deploy-snapshot.sh");

      LOG.info("writing {}", filePath);

      try (var out = Files.newOutputStream(filePath, FILE_WRITE_OPTIONS)) {
        try (var in = GHRTCommandWorkflows.class
          .getResourceAsStream("/com/io7m/ghrepostools/deploy-snapshot.sh")) {
          in.transferTo(out);
          out.flush();
        }
      }

      Files.setPosixFilePermissions(
        filePath,
        PosixFilePermissions.fromString("rwx------")
      );
    }

    {
      final var filePath =
        workflowDirectory.resolve("deploy-release.sh");

      LOG.info("writing {}", filePath);

      try (var out = Files.newOutputStream(filePath, FILE_WRITE_OPTIONS)) {
        try (var in = GHRTCommandWorkflows.class
          .getResourceAsStream("/com/io7m/ghrepostools/deploy-release.sh")) {
          in.transferTo(out);
          out.flush();
        }
      }

      Files.setPosixFilePermissions(
        filePath,
        PosixFilePermissions.fromString("rwx------")
      );
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
