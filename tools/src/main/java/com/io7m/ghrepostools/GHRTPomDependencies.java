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

public final class GHRTPomDependencies
{
  private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
    DocumentBuilderFactory.newInstance();

  private GHRTPomDependencies()
  {

  }

  public static GHRTPomNode pomDependencies(
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

    final String groupName =
      findGroup(root);
    final String artifactName =
      findArtifact(root);
    final var identifier =
      new GHRTPomIdentifier(groupName, artifactName);

    final var dependenciesNodes =
      root.getElementsByTagName("dependencies");

    if (dependenciesNodes.getLength() < 1) {
      return new GHRTPomNode(identifier, List.of());
    }

    final var dependenciesNode =
      (Element) dependenciesNodes.item(0);

    final var dependencyNodes =
      dependenciesNode.getElementsByTagName("dependency");

    final var dependencies =
      new ArrayList<GHRTPomIdentifier>();

    for (int index = 0; index < dependencyNodes.getLength(); ++index) {
      final var dependencyNode = (Element) dependencyNodes.item(index);
      Preconditions.checkPrecondition(
        dependencyNode.getTagName(),
        e -> Objects.equals(dependencyNode.getTagName(), "dependency"),
        e -> "Node must be 'dependency'"
      );

      var groupNode =
        dependencyNode.getElementsByTagName("groupId")
          .item(0)
          .getTextContent()
          .trim();

      if ("${project.groupId}".equals(groupNode)) {
        groupNode = groupName;
      }

      final var artifactNode =
        dependencyNode.getElementsByTagName("artifactId")
          .item(0)
          .getTextContent()
          .trim();

      dependencies.add(new GHRTPomIdentifier(groupNode, artifactNode));
    }

    return new GHRTPomNode(identifier, List.copyOf(dependencies));
  }

  private static String findArtifact(
    final Element root)
  {
    final var childNodes = root.getChildNodes();
    for (int index = 0; index < childNodes.getLength(); ++index) {
      final var node = childNodes.item(index);
      if (node instanceof final Element element) {
        if (Objects.equals(element.getTagName(), "artifactId")) {
          return element.getTextContent().trim();
        }
      }
    }

    throw new IllegalStateException("Missing an artifactId node!");
  }

  private static String findGroup(
    final Element root)
  {
    {
      final var childNodes = root.getChildNodes();
      for (int index = 0; index < childNodes.getLength(); ++index) {
        final var node = childNodes.item(index);
        if (node instanceof final Element element) {
          if (Objects.equals(element.getTagName(), "groupId")) {
            return element.getTextContent().trim();
          }
        }
      }
    }

    final var parentNode =
      (Element) root.getElementsByTagName("parent")
        .item(0);

    final var childNodes = parentNode.getChildNodes();
    for (int index = 0; index < childNodes.getLength(); ++index) {
      final var node = childNodes.item(index);
      if (node instanceof final Element element) {
        if (Objects.equals(element.getTagName(), "groupId")) {
          return element.getTextContent().trim();
        }
      }
    }

    throw new IllegalStateException("Missing a groupId node!");
  }
}
