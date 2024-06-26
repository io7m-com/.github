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
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class GHRTCommandGenDepChanges implements QCommandType
{
  private static final QParameterNamed01<Path> OUTPUT =
    new QParameterNamed01<>(
      "--output",
      List.of(),
      new QStringType.QConstant("The output file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public GHRTCommandGenDepChanges()
  {
    this.metadata = new QCommandMetadata(
      "generate-changelog",
      new QStringType.QConstant("Generate a series of shell commands for changelog."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(OUTPUT);
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final PrintStream outputWriter;
    if (context.parameterValue(OUTPUT).isPresent()) {
      final var path =
        context.parameterValue(OUTPUT).orElseThrow();
      outputWriter =
        new PrintStream(Files.newOutputStream(path), true, UTF_8);
    } else {
      outputWriter = System.out;
    }

    final var u =
      GHRTCommandGenDepChanges.class.getResource(
        "/com/io7m/ghrepostools/versions-plain-changelog-cmds.xslt");

    try (var s = u.openStream()) {
      final var transformers =
        TransformerFactory.newInstance();
      final var transformer =
        transformers.newTransformer(new StreamSource(s));
      transformer.transform(
        new StreamSource("target/use-latest-releases.xml"),
        new StreamResult(outputWriter)
      );
    } catch (final Exception e) {
      // Don't care
    }

    try {
      try (var s = u.openStream()) {
        final var transformers =
          TransformerFactory.newInstance();
        final var transformer =
          transformers.newTransformer(new StreamSource(s));
        transformer.transform(
          new StreamSource("target/update-properties.xml"),
          new StreamResult(outputWriter)
        );
      }
    } catch (final Exception e) {
      // Don't care
    }

    System.out.println();
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
