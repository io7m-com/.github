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
import com.io7m.quarrel.core.QParameterNamed0N;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class GHRTCommandShowInTopologicalOrder implements QCommandType
{
  private final QCommandMetadata metadata;

  private static final QParameterNamed0N<Path> INPUT =
    new QParameterNamed0N<>(
      "--pom-file",
      List.of(),
      new QStringType.QConstant("The input POM file(s)."),
      List.of(),
      Path.class
    );

  private static final QParameterNamed01<Path> SEARCH_ROOT =
    new QParameterNamed01<>(
      "--pom-search",
      List.of(),
      new QStringType.QConstant("The root directory for searching for POM files."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<String> FILTER_GROUP =
    new QParameterNamed1<>(
      "--filter-group",
      List.of(),
      new QStringType.QConstant("The pattern against which to filter group names."),
      Optional.of(".*"),
      String.class
    );

  /**
   * Construct a command.
   */

  public GHRTCommandShowInTopologicalOrder()
  {
    this.metadata = new QCommandMetadata(
      "show-in-topological-order",
      new QStringType.QConstant("Show projects in topological order."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(INPUT, SEARCH_ROOT, FILTER_GROUP);
  }

  private record GraphGroupEdge(
    String source,
    String target) {

  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var files = new TreeSet<Path>();
    for (final var file : context.parameterValues(INPUT)) {
      files.add(file.toAbsolutePath());
    }

    final var rootOpt = context.parameterValue(SEARCH_ROOT);
    if (rootOpt.isPresent()) {
      final var searchRoot = rootOpt.orElseThrow();
      try (var walk = Files.walk(searchRoot)) {
        walk.filter(f -> "pom.xml".equals(f.getFileName().toString()))
          .map(Path::toAbsolutePath)
          .forEach(files::add);
      }
    }

    final var graphPattern =
      Pattern.compile(context.parameterValue(FILTER_GROUP));

    final var graph =
      new DirectedAcyclicGraph<String, GraphGroupEdge>(GraphGroupEdge.class);

    for (final var file : files) {
      final var node =
        GHRTPomDependencies.pomDependencies(file);
      final var groupName =
        node.node().groupName();

      graph.addVertex(groupName);
      for (final var id : node.dependencies()) {
        graph.addVertex(id.groupName());
      }

      for (final var id : node.dependencies()) {
        final var target = id.groupName();
        if (Objects.equals(groupName, target)) {
          continue;
        }

        final var edge = new GraphGroupEdge(groupName, target);
        graph.addEdge(groupName, target, edge);
      }
    }

    final var output =
      context.output();
    final var topo =
      new TopologicalOrderIterator<>(graph);
    final var toShow =
      new ArrayList<String>();

    while (topo.hasNext()) {
      final var node = topo.next();
      if (!graphPattern.matcher(node).matches()) {
        continue;
      }
      toShow.add(node);
    }

    Collections.reverse(toShow);
    for (final var name : toShow) {
      System.out.println(name);
    }

    output.flush();
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
