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


package com.io7m.ghrepostools.templating;

import java.awt.Color;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record GHRTReadmeModel(
  String groupNameWithDots,
  String artifactNameWithDots,
  String groupNameWithSlashes,
  String shortName,
  String javaVersion)
  implements GHRTTemplateDataModelType
{
  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("encodedSnapshotURL", this.encodedSnapshotURL());
    m.put("groupNameWithDots", this.groupNameWithDots());
    m.put("artifactNameWithDots", this.artifactNameWithDots());
    m.put("groupNameWithSlashes", this.groupNameWithSlashes());
    m.put("shortName", this.shortName());
    m.put("javaVersion", this.javaVersion());
    m.put("javaVersionColor", this.javaVersionColor());
    return m;
  }

  private String encodedSnapshotURL()
  {
    final var baseRepoURI =
      URI.create("https://central.sonatype.com/repository/maven-snapshots/");

    var repoURI = baseRepoURI;
    repoURI = repoURI.resolve(this.groupNameWithSlashes + "/");
    repoURI = repoURI.resolve(this.artifactNameWithDots + "/");
    repoURI = repoURI.resolve("maven-metadata.xml");
    repoURI = repoURI.normalize();

    return URLEncoder.encode(repoURI.toString(), StandardCharsets.UTF_8);
  }

  private String javaVersionColor()
  {
    final var javaMin = 11.0;
    final var javaMax = 21.0 + 6.0;

    final var v =
      Math.clamp(
        (double) Integer.parseUnsignedInt(this.javaVersion),
        javaMin,
        javaMax
      );

    final var angle =
      (v - javaMin) / (javaMax - javaMin);

    final var color =
      Color.getHSBColor(
        (float) angle + 0.5f,
        0.6F,
        0.9F
      );

    return String.format(
      "%06x",
      Integer.valueOf(color.getRGB() & 0xffffff)
    );
  }
}
