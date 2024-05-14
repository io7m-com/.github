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

import java.util.List;

import static com.io7m.ghrepostools.GHRTCoverageEnabled.COVERAGE_DISABLED;
import static com.io7m.ghrepostools.GHRTCoverageEnabled.COVERAGE_ENABLED;
import static com.io7m.ghrepostools.GHRTDeployEnabled.DEPLOY_DISABLED;
import static com.io7m.ghrepostools.GHRTDeployEnabled.DEPLOY_ENABLED;
import static com.io7m.ghrepostools.GHRTJDKCategory.CURRENT;
import static com.io7m.ghrepostools.GHRTJDKCategory.LTS;
import static com.io7m.ghrepostools.GHRTJDKDistribution.TEMURIN;
import static com.io7m.ghrepostools.GHRTPlatform.LINUX;
import static com.io7m.ghrepostools.GHRTPlatform.WINDOWS;

public final class GHRTWorkflows
{
  public GHRTWorkflows()
  {

  }

  public static int JDK_CURRENT = 22;
  public static int JDK_LTS = 21;

  public List<GHRTWorkflow> workflows()
  {
    return List.of(
      new GHRTWorkflow(
        LINUX,
        TEMURIN,
        CURRENT,
        JDK_CURRENT,
        COVERAGE_DISABLED,
        DEPLOY_DISABLED
      ),
      new GHRTWorkflow(
        LINUX,
        TEMURIN,
        LTS,
        JDK_LTS,
        COVERAGE_ENABLED,
        DEPLOY_ENABLED
      ),
      new GHRTWorkflow(
        WINDOWS,
        TEMURIN,
        CURRENT,
        JDK_CURRENT,
        COVERAGE_DISABLED,
        DEPLOY_DISABLED
      ),
      new GHRTWorkflow(
        WINDOWS,
        TEMURIN,
        LTS,
        JDK_LTS,
        COVERAGE_DISABLED,
        DEPLOY_DISABLED
      )
    );
  }
}
