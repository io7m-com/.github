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

import com.io7m.quarrel.core.QApplication;
import com.io7m.quarrel.core.QApplicationMetadata;
import com.io7m.quarrel.core.QApplicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Main command line entry point.
 */

public final class Main implements Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Main.class);

  private final String[] args;
  private final QApplicationType application;
  private final int exitCode;

  /**
   * The main entry point.
   *
   * @param inArgs Command-line arguments
   */

  public Main(
    final String[] inArgs)
  {
    this.args =
      Objects.requireNonNull(inArgs, "Command line arguments");

    final var metadata =
      new QApplicationMetadata(
        "ghrepostools",
        "com.io7m.ghrepostools",
        "0.0.1",
        "8ccbaad780c02805263297ad4b7dce4589a0c3ec",
        "GH Repository Tools",
        Optional.of(URI.create("https://www.github.com/io7m-com/.github"))
      );

    final var builder =
      QApplication.builder(metadata);

    builder.addCommand(new GHRTCommandDependabot());
    builder.addCommand(new GHRTCommandExecuteRelease());
    builder.addCommand(new GHRTCommandGenDepChanges());
    builder.addCommand(new GHRTCommandPushIO7MSite());
    builder.addCommand(new GHRTCommandReadme());
    builder.addCommand(new GHRTCommandShowDepCommit());
    builder.addCommand(new GHRTCommandShowDependencies());
    builder.addCommand(new GHRTCommandShowDependencyGraph());
    builder.addCommand(new GHRTCommandShowInTopologicalOrder());
    builder.addCommand(new GHRTCommandUpdateChangelog());
    builder.addCommand(new GHRTCommandWorkflows());

    this.application = builder.build();
    this.exitCode = 0;
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(
    final String[] args)
  {
    System.exit(mainExitless(args));
  }

  /**
   * The main (exitless) entry point.
   *
   * @param args Command line arguments
   *
   * @return The exit code
   */

  public static int mainExitless(
    final String[] args)
  {
    final Main cm = new Main(args);
    cm.run();
    return cm.exitCode();
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exitCode;
  }

  @Override
  public void run()
  {
    this.application.run(LOG, List.of(this.args));
  }

  @Override
  public String toString()
  {
    return String.format(
      "[Main 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
