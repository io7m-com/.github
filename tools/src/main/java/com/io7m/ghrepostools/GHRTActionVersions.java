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
       /* https://github.com/actions/checkout/releases */
      "08c6903cd8c0fde910a37f88322edcfb5dd907a8",
      /* https://github.com/actions/setup-java/releases */
      "dded0888837ed1f317902acf8a20df0ad188d165",
      /* https://github.com/actions/upload-artifact/releases */
      "330a01c490aca151604b8cf639adc76d48f6c5d4",
      /* https://github.com/codecov/codecov-action/releases */
      "5a1091511ad55cbe89839c7260b706298ca349f7",
      /* https://github.com/redhat-actions/podman-login/releases */
      "4934294ad0449894bcd1e9f191899d7292469603"
    );
  }
}
