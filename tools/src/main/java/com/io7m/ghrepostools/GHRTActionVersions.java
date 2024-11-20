/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import java.util.Objects;

public record GHRTActionVersions(
  String checkoutVersion,
  String setupJavaVersion,
  String uploadArtifactVersion,
  String codecovVersion,
  String podmanLoginVersion)
{
  public GHRTActionVersions
  {
    Objects.requireNonNull(checkoutVersion, "checkoutVersion");
    Objects.requireNonNull(setupJavaVersion, "setupJavaVersion");
    Objects.requireNonNull(uploadArtifactVersion, "uploadArtifactVersion");
    Objects.requireNonNull(codecovVersion, "codecovVersion");
    Objects.requireNonNull(podmanLoginVersion, "podmanLoginVersion");
  }

  public static GHRTActionVersions get()
  {
    return new GHRTActionVersions(
      "4",
      "4",
      "4",
      "5.0.5",
      "1.7"
    );
  }
}
