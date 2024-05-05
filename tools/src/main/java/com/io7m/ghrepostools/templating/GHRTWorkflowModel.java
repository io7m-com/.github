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

import java.util.HashMap;
import java.util.Map;

public record GHRTWorkflowModel(
  String workflowName,
  String imageName,
  String javaVersion,
  String javaDistribution,
  String projectName,
  boolean coverage,
  boolean deploy,
  String sourceEvent)
  implements GHRTTemplateDataModelType
{
  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();
    m.put("workflowName", this.workflowName());
    m.put("imageName", this.imageName());
    m.put("javaVersion", this.javaVersion());
    m.put("javaDistribution", this.javaDistribution());
    m.put("projectName", this.projectName());
    m.put("coverage", this.coverage());
    m.put("deploy", this.deploy());
    m.put("sourceEvent", this.sourceEvent());
    return m;
  }
}
