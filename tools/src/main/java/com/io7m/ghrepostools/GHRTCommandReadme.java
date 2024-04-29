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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public final class GHRTCommandReadme implements QCommandType
{
  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public GHRTCommandReadme()
  {
    this.metadata = new QCommandMetadata(
      "readme",
      new QStringType.QConstant("Generate a README file."),
      Optional.empty()
    );
  }

  private static URI workflowURI(
    final GHRTProjectName names,
    final GHRTWorkflow workflow)
  {
    return URI.create(
      "https://www.github.com/io7m-com/%s/actions?query=workflow%%3A%s"
        .formatted(names.shortName(), workflow.name())
    );
  }

  private static URI shieldsURI(
    final GHRTProjectName names,
    final GHRTWorkflow workflow)
  {
    return URI.create(
      "https://img.shields.io/github/actions/workflow/status/io7m-com/%s/%s.yml"
        .formatted(names.shortName(), workflow.name())
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
        GHRTCommandReadme.class,
        "/com/io7m/ghrepostools/Strings.xml");

    final var slashGroup =
      names.groupName().stream().collect(Collectors.joining("/"));
    final var dotGroup =
      names.groupName().stream().collect(Collectors.joining("."));

    System.out.println(MessageFormat.format(
      resources.getString("readmeTemplate"),
      dotGroup,
      slashGroup,
      names.projectName(),
      names.shortName()
    ));

    final var workflows = new GHRTWorkflows().workflows();
    for (final var workflow : workflows) {
      final var row = new StringBuilder();
      row.append(
        "| OpenJDK (%s) %s | %s | "
          .formatted(
            workflow.jdkDistribution().humanName(),
            workflow.jdkCategory().humanName(),
            workflow.platform().humanName()
          )
      );
      row.append(
        "[![Build (OpenJDK (%s) %s, %s)](%s)](%s)|"
          .formatted(
            workflow.jdkDistribution().humanName(),
            workflow.jdkCategory().humanName(),
            workflow.platform().humanName(),
            shieldsURI(names, workflow),
            workflowURI(names, workflow)
          )
      );

      System.out.println(row);
    }

    final var extra = Paths.get("README.in");
    if (Files.isRegularFile(extra)) {
      System.out.println(Files.readString(extra));
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
