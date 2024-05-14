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

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GHRTWorkflowProfile(
  String name,
  GHRTVideoRecordingEnabled videoRecordingEnabled,
  GHRTCustomRunScriptEnabled customRunScriptEnabled)
{
  public GHRTWorkflowProfile
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(videoRecordingEnabled, "videoRecordingEnabled");
  }

  public static Map<String, GHRTWorkflowProfile> profiles()
  {
    return Stream.of(
      core(),
      screenRecorded(),
      customRun()
    ).collect(Collectors.toMap(c -> c.name, c -> c));
  }

  private static GHRTWorkflowProfile customRun()
  {
    return new GHRTWorkflowProfile(
      "CustomRunScript",
      GHRTVideoRecordingEnabled.VIDEO_RECORDING_DISABLED,
      GHRTCustomRunScriptEnabled.CUSTOM_RUN_SCRIPT_ENABLED
    );
  }

  public static GHRTWorkflowProfile screenRecorded()
  {
    return new GHRTWorkflowProfile(
      "ScreenRecorded",
      GHRTVideoRecordingEnabled.VIDEO_RECORDING_ENABLED,
      GHRTCustomRunScriptEnabled.CUSTOM_RUN_SCRIPT_DISABLED
    );
  }

  public static GHRTWorkflowProfile core()
  {
    return new GHRTWorkflowProfile(
      "Core",
      GHRTVideoRecordingEnabled.VIDEO_RECORDING_DISABLED,
      GHRTCustomRunScriptEnabled.CUSTOM_RUN_SCRIPT_DISABLED
    );
  }
}
