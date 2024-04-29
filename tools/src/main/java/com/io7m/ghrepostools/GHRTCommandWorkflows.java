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

    {
      var path = Path.of("");
      path = path.resolve(".github");
      path = path.resolve("workflows");

      Files.createDirectories(path);

      final var options = new OpenOption[]{
        WRITE, TRUNCATE_EXISTING, CREATE
      };

      final var workflows = new GHRTWorkflows().workflows();
      for (final var workflow : workflows) {
        final var filePath =
          path.resolve("%s.yml".formatted(workflow.name()));

        LOG.info("writing {}", filePath);

        try (var output =
               Files.newBufferedWriter(filePath, options)) {
          output.append(
            MessageFormat.format(
              resources.getString("workflowTemplate"),
              workflow.name(),
              workflow.platform().imageName(),
              Integer.valueOf(workflow.jdkVersion()),
              "'" + workflow.jdkDistribution().lowerName() + "'",
              names.shortName()
            )
          );

          if (workflow.coverage()) {
            output.append(
              MessageFormat.format(
                resources.getString("coverageTemplate"),
                names.shortName()
              )
            );
          }

          output.newLine();
          output.flush();
        }
      }
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
