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

import com.io7m.jaffirm.core.Preconditions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GHRTPomJavaVersion
{
  private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
    DocumentBuilderFactory.newInstance();

  private GHRTPomJavaVersion()
  {

  }

  public static String pomJavaVersion(
    final Path file)
    throws Exception
  {
    final var docBuilder =
      DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

    final Document document;
    try (var stream = Files.newInputStream(file)) {
      document = docBuilder.parse(stream, file.toString());
    }

    final var root = document.getDocumentElement();
    Preconditions.checkPrecondition(
      root.getTagName(),
      e -> Objects.equals(root.getTagName(), "project"),
      e -> "Root node must be 'project'"
    );

    return document.getElementsByTagName("io7m.java.targetJavaVersion")
      .item(0)
      .getTextContent();
  }
}
