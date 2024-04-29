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
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed0N;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public final class GHRTCommandShowDependencies implements QCommandType
{
  private final QCommandMetadata metadata;

  private static final QParameterNamed0N<Path> INPUT =
    new QParameterNamed0N<>(
      "--pom-file",
      List.of(),
      new QStringType.QConstant("The input POM file."),
      List.of(),
      Path.class
    );

  private static final QParameterNamed01<Path> SEARCH_ROOT =
    new QParameterNamed01<>(
      "--pom-search",
      List.of(),
      new QStringType.QConstant("The root directory for searching for POM files."),
      Optional.empty(),
      Path.class
    );

  /**
   * Construct a command.
   */

  public GHRTCommandShowDependencies()
  {
    this.metadata = new QCommandMetadata(
      "show-dependencies",
      new QStringType.QConstant("List dependencies in a POM file."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(INPUT, SEARCH_ROOT);
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var files = new TreeSet<Path>();
    for (final var file : context.parameterValues(INPUT)) {
      files.add(file.toAbsolutePath());
    }

    final var rootOpt = context.parameterValue(SEARCH_ROOT);
    if (rootOpt.isPresent()) {
      final var searchRoot = rootOpt.orElseThrow();
      try (var walk = Files.walk(searchRoot)) {
        walk.filter(f -> "pom.xml".equals(f.getFileName().toString()))
          .map(Path::toAbsolutePath)
          .forEach(files::add);
      }
    }

    final var dependencies = new TreeSet<GHRTPomIdentifier>();
    for (final var file : files) {
      dependencies.addAll(
        GHRTPomDependencies.pomDependencies(file)
          .dependencies()
      );
    }

    final var output = context.output();
    for (final var dependency : dependencies) {
      output.printf(
        "%s:%s%n",
        dependency.groupName(),
        dependency.artifactName()
      );
    }
    output.flush();
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
