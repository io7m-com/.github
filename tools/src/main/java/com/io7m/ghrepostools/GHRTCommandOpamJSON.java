/*
 * Copyright © 2025 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.io7m.ghrepostools.opam.OPAMFile;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class GHRTCommandOpamJSON implements QCommandType
{
  private static final QParameterNamed1<Path> OPAM_JSON =
    new QParameterNamed1<>(
      "--opam-json",
      List.of(),
      new QStringType.QConstant("The source OPAM JSON file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OPAM =
    new QParameterNamed1<>(
      "--opam",
      List.of(),
      new QStringType.QConstant("The target OPAM file."),
      Optional.empty(),
      Path.class
    );

  private QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public GHRTCommandOpamJSON()
  {
    this.metadata = new QCommandMetadata(
      "opam-json",
      new QStringType.QConstant("Convert OPAM JSON files to OPAM."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(OPAM_JSON, OPAM);
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var source =
      context.parameterValue(OPAM_JSON);
    final var target =
      context.parameterValue(OPAM);

    final var mapper =
      JsonMapper.builder()
        .build();

    final var opam =
      mapper.readValue(source.toFile(), OPAMFile.class);

    try (final var writer = Files.newBufferedWriter(target, UTF_8)) {
      writer.append("# Auto generated. Do not edit.%n".formatted());
      writer.newLine();

      writer.append(
        "opam-version: \"%s\"%n".formatted(opam.opamVersion())
      );
      writer.append(
        "name: \"%s\"%n".formatted(opam.name())
      );
      writer.append(
        "version: \"%s\"%n".formatted(opam.version())
      );
      writer.newLine();

      writer.append(
        "authors: \"%s\"%n".formatted(opam.authors())
      );
      writer.append(
        "bug-reports: \"%s\"%n".formatted(opam.bugReports())
      );
      writer.append(
        "dev-repo: \"%s\"%n".formatted(opam.devRepos())
      );
      writer.append(
        "homepage: \"%s\"%n".formatted(opam.homePage())
      );
      writer.append(
        "license: \"%s\"%n".formatted(opam.license())
      );
      writer.append(
        "maintainer: \"%s\"%n".formatted(opam.maintainer())
      );
      writer.append(
        "synopsis: \"%s\"%n".formatted(opam.synopsis())
      );
      writer.append(
        "x-io7m-work-id: \"%s\"%n".formatted(opam.io7mWorkId())
      );
      writer.append(
        "x-io7m-documentation: \"%s\"%n".formatted(opam.io7mDocumentation())
      );
      writer.newLine();

      writer.append(
        "depends: [%n".formatted()
      );
      for (final var depends : opam.depends()) {
        writer.append(
          "  \"%s\" {= \"%s\"}%n".formatted(depends.name(), depends.version())
        );
      }
      writer.append(
        "]%n".formatted()
      );
      writer.newLine();

      writer.append(
        "build: [%n".formatted()
      );
      for (final var exec : opam.build()) {
        writer.append("  [");
        writer.append(
          exec.stream()
            .map("\"%s\""::formatted)
            .collect(Collectors.joining(" "))
        );
        writer.append("]");
        writer.newLine();
      }
      writer.append(
        "]%n".formatted()
      );
      writer.newLine();

      writer.append(
        "install: [%n".formatted()
      );
      for (final var exec : opam.install()) {
        writer.append("  [");
        writer.append(
          exec.stream()
            .map("\"%s\""::formatted)
            .collect(Collectors.joining(" "))
        );
        writer.append("]");
        writer.newLine();
      }
      writer.append(
        "]%n".formatted()
      );
      writer.newLine();

      writer.flush();
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
