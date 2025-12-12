/*
 * Copyright © 2025 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.ghrepostools.opam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record OPAMFile(
  @JsonProperty(value = "opam-version", required = true)
  String opamVersion,
  @JsonProperty(value = "name", required = true)
  String name,
  @JsonProperty(value = "version", required = true)
  String version,
  @JsonProperty(value = "authors", required = true)
  String authors,
  @JsonProperty(value = "bug-reports", required = true)
  String bugReports,
  @JsonProperty(value = "dev-repo", required = true)
  String devRepos,
  @JsonProperty(value = "homepage", required = true)
  String homePage,
  @JsonProperty(value = "license", required = true)
  String license,
  @JsonProperty(value = "maintainer", required = true)
  String maintainer,
  @JsonProperty(value = "synopsis", required = true)
  String synopsis,
  @JsonProperty(value = "x-io7m-work-id", required = true)
  String io7mWorkId,
  @JsonProperty(value = "x-io7m-documentation", required = true)
  String io7mDocumentation,
  @JsonProperty(value = "depends")
  List<OPAMDependency> depends,
  @JsonProperty(value = "install")
  List<List<String>> install)
{

}
