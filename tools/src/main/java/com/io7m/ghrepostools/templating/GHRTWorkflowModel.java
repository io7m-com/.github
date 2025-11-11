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

import com.io7m.ghrepostools.GHRTActionVersions;
import com.io7m.ghrepostools.GHRTCoverageEnabled;
import com.io7m.ghrepostools.GHRTCustomRunScript;
import com.io7m.ghrepostools.GHRTCustomRunScriptEnabled;
import com.io7m.ghrepostools.GHRTDeployEnabled;
import com.io7m.ghrepostools.GHRTVideoRecordingEnabled;
import com.io7m.ghrepostools.GHRTVulkanEnabled;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record GHRTWorkflowModel(
  GHRTActionVersions actionVersions,
  String workflowProfileName,
  String workflowName,
  String platformName,
  String imageName,
  String javaVersion,
  String javaDistribution,
  String projectName,
  GHRTCoverageEnabled coverage,
  GHRTDeployEnabled deploy,
  GHRTVideoRecordingEnabled videoRecordingEnabled,
  GHRTVulkanEnabled vulkanEnabled,
  Optional<GHRTCustomRunScript> customRunScript,
  String sourceEvent)
  implements GHRTTemplateDataModelType
{
  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>();

    final var v = this.actionVersions;
    m.put("actionsCheckoutVersion", v.checkoutVersion());
    m.put("actionsCodecovVersion", v.codecovVersion());
    m.put("actionsPodmanLoginVersion", v.podmanLoginVersion());
    m.put("actionsSetupJavaVersion", v.setupJavaVersion());
    m.put("actionsUploadArtifactVersion", v.uploadArtifactVersion());

    this.customRunScript.ifPresent(script -> {
      m.put("customRunScript", script.name());
    });

    m.put("coverage", this.coverage());
    m.put("deploy", this.deploy());
    m.put("imageName", this.imageName());
    m.put("platformName", this.platformName());
    m.put("javaDistribution", this.javaDistribution());
    m.put("javaVersion", this.javaVersion());
    m.put("projectName", this.projectName());
    m.put("sourceEvent", this.sourceEvent());
    m.put("videoRecordingEnabled", this.videoRecordingEnabled);
    m.put("vulkanEnabled", this.vulkanEnabled);
    m.put("workflowName", this.workflowName());
    m.put("workflowProfileName", this.workflowProfileName());
    return m;
  }
}
