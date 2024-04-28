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

import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@Parameters(commandDescription = "Generate dependabot files.")
public final class GHRTCommandDependabot extends CLPAbstractCommand
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHRTCommandDependabot.class);

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public GHRTCommandDependabot(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var names =
      GHRTProjectNames.projectName();

    final var resources =
      GHRTStrings.ofXMLResource(
        GHRTCommandDependabot.class,
        "/com/io7m/ghrepostools/Strings.xml"
      );

    {
      var path = Path.of("");
      path = path.resolve(".github");
      path = path.resolve("dependabot-is-custom");

      if (Files.exists(path)) {
        return SUCCESS;
      }
    }

    {
      var path = Path.of("");
      path = path.resolve(".github");
      path = path.resolve("dependabot.yml");

      Files.createDirectories(path.getParent());

      final var options = new OpenOption[]{
        WRITE, TRUNCATE_EXISTING, CREATE
      };

      Files.writeString(
        path,
        resources.getString("dependabotTemplate")
      );
    }

    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "dependabot";
  }
}
