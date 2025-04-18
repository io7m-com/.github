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
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class GHRTCommandPushIO7MSite implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandPushIO7MSite.class);

  private final QCommandMetadata metadata;

  private static final QParameterNamed1<String> PROJECT =
    new QParameterNamed1<>(
      "--project",
      List.of(),
      new QStringType.QConstant("The project name."),
      Optional.empty(),
      String.class
    );

  /**
   * Construct a command.
   */

  public GHRTCommandPushIO7MSite()
  {
    this.metadata =
      new QCommandMetadata(
        "push-io7m-site",
        new QStringType.QConstant("Push a release site."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      PROJECT
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var project =
      context.parameterValue(PROJECT);

    final var siteDirectory =
      Paths.get(project + "-SITE")
        .toAbsolutePath();

    if (!Files.isDirectory(siteDirectory)) {
      throw new IllegalStateException(
        "A directory %s already exists".formatted(siteDirectory)
      );
    }

    executeProgram(
      Paths.get("."),
      "rsync",
      "--delete",
      "-a",
      "-zz",
      "-c",
      "-L",
      "--chmod=ugo-rwx,Dugo+x,ugo+r,u+w",
      "--progress",
      siteDirectory + "/",
      "web03.int.io7m.com:/data/www/www.io7m.com/software/%s/"
        .formatted(project)
    );

    return QCommandStatus.SUCCESS;
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
