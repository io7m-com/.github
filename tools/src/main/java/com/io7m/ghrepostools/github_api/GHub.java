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

package com.io7m.ghrepostools.github_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public final class GHub implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHub.class);

  private final HttpClient client;
  private final String token;
  private final JsonMapper json;

  private GHub(
    final HttpClient client,
    final String token)
  {
    this.client = client;
    this.token = token;
    this.json =
      JsonMapper.builder()
        .build();
  }

  public static GHub create()
    throws Exception
  {
    final var homeDir =
      Paths.get(System.getProperty("user.home"));
    final var propertyFile =
      homeDir.resolve(".github");

    final var properties = new Properties();
    try (final var stream = Files.newInputStream(propertyFile)) {
      properties.load(stream);
    }

    final var token = properties.getProperty("oauth");
    if (token == null) {
      throw new IllegalStateException("Missing GitHub OAuth token.");
    }

    return new GHub(HttpClient.newHttpClient(), token);
  }

  public List<GPullRequest> pullRequests(
    final String reposName)
    throws Exception
  {
    final var target =
      URI.create(
        "https://api.github.com/repos/%s/pulls"
          .formatted(reposName)
      );
    return this.doGET(
      target, new TypeReference<List<GPullRequest>>()
      {

      }).data;
  }

  public GPullRequest pullRequestFull(
    final String reposName,
    final BigInteger number)
    throws Exception
  {
    final var target =
      URI.create(
        "https://api.github.com/repos/%s/pulls/%s"
          .formatted(reposName, number)
      );
    return this.doGET(
      target, new TypeReference<GPullRequest>()
      {

      }).data;
  }

  private record Result<T>(
    T data,
    HttpHeaders headers)
  {

  }

  private <T> Result<T> doGET(
    final URI target,
    final TypeReference<T> type)
    throws IOException, InterruptedException
  {
    final var request =
      HttpRequest.newBuilder(target)
        .header("Authorization", "Bearer %s".formatted(this.token))
        .header("X-GitHub-Api-Version", "2022-11-28")
        .GET()
        .build();

    final var response =
      this.client.send(
        request,
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );

    dumpIncomingHeaders(response);
    this.dumpResponse(response);

    if (response.statusCode() >= 300) {
      throw new IOException(
        "%s: Status %s".formatted(target, response.statusCode())
      );
    }

    return new Result<>(
      this.json.readValue(response.body(), type),
      response.headers()
    );
  }

  private <T> T doPUT(
    final URI target,
    final byte[] body,
    final TypeReference<T> type)
    throws IOException, InterruptedException
  {
    final var request =
      HttpRequest.newBuilder(target)
        .header("Authorization", "Bearer %s".formatted(this.token))
        .header("X-GitHub-Api-Version", "2022-11-28")
        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
        .build();

    final var response =
      this.client.send(
        request,
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );

    dumpIncomingHeaders(response);
    this.dumpResponse(response);

    if (response.statusCode() >= 300) {
      final var error =
        this.json.readValue(response.body(), GError.class);

      throw new IOException(
        "%s: Status %s: %s (%s)"
          .formatted(
            target,
            response.statusCode(),
            error.message,
            error.documentationURL
          )
      );
    }

    return this.json.readValue(response.body(), type);
  }

  private static void dumpIncomingHeaders(
    final HttpResponse<String> response)
  {
    for (final var header : response.headers().map().entrySet()) {
      for (final var value : header.getValue()) {
        LOG.debug("< {}: {}", header.getKey(), value);
      }
    }
  }

  public List<GCommit> pullRequestCommits(
    final GPullRequest pullRequest)
    throws Exception
  {
    return this.doGET(
      pullRequest.commitsURL, new TypeReference<List<GCommit>>()
      {

      }).data;
  }

  private void dumpResponse(
    final HttpResponse<String> response)
    throws JsonProcessingException
  {
    LOG.trace(
      "{}",
      this.json.writerWithDefaultPrettyPrinter()
        .writeValueAsString(this.json.readTree(response.body()))
    );
  }

  public GWorkflowRuns pullRequestWorkflowRuns(
    final String reposName,
    final GPullRequest pr,
    final GCommit commit)
    throws IOException, InterruptedException
  {
    final var target =
      URI.create(
        "https://api.github.com/repos/%s/actions/runs?head_sha=%s"
          .formatted(reposName, commit.sha)
      );

    return this.doGET(
      target,
      new TypeReference<GWorkflowRuns>()
      {

      }).data;
  }

  @Override
  public void close()
  {
    this.client.close();
  }

  public GPullRequestMergeResponse pullRequestMerge(
    final String reposName,
    final GPullRequest pr)
    throws IOException, InterruptedException
  {
    final var target =
      URI.create(
        "https://api.github.com/repos/%s/pulls/%s/merge"
          .formatted(reposName, pr.number())
      );

    final var data =
      this.json.createObjectNode();

    return this.doPUT(
      target,
      this.json.writeValueAsBytes(data),
      new TypeReference<GPullRequestMergeResponse>()
      {

      });
  }

  public List<GRepository> repositories(
    final Pattern reposInclude,
    final Pattern reposExclude)
    throws IOException, InterruptedException
  {
    final List<GRepository> repositories =
      new ArrayList<>();

    URI target =
      URI.create("https://api.github.com/user/repos?per_page=100");

    while (target != null) {
      final var r =
        this.doGET(
          target, new TypeReference<List<GRepository>>()
          {
          });

      repositories.addAll(r.data);
      target = findNext(r);
    }

    LOG.debug("Retrieved {} repositories", repositories.size());
    repositories.removeIf(r -> {
      return !reposInclude.matcher(r.fullName).matches();
    });
    repositories.removeIf(r -> {
      return reposExclude.matcher(r.fullName).matches();
    });
    LOG.debug("{} repositories remain after filtering", repositories.size());
    return repositories;
  }

  private static final Pattern LINK_PATTERN =
    Pattern.compile("""
<(.*)>; rel="next"
""".trim());

  private static URI findNext(
    final Result<?> result)
  {
    final var headers = result.headers;
    final var linkOpt = headers.firstValue("link");
    if (linkOpt.isEmpty()) {
      return null;
    }

    final var linkData =
      linkOpt.get();
    final var linkParts =
      Arrays.stream(linkData.split(","))
        .map(String::trim)
        .toList();

    for (final var link : linkParts) {
      final var matcher = LINK_PATTERN.matcher(link);
      if (matcher.matches()) {
        return URI.create(matcher.group(1));
      }
    }
    return null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GRepository(
    @JsonProperty("id") BigInteger id,
    @JsonProperty("full_name") String fullName)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GError(
    @JsonProperty("message") String message,
    @JsonProperty("documentation_url") String documentationURL,
    @JsonProperty("status") String status)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GPullRequestMergeResponse(
    @JsonProperty("sha") String sha,
    @JsonProperty("merged") boolean merged,
    @JsonProperty("message") String message)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GWorkflowRuns(
    @JsonProperty("total_count") BigInteger totalCount,
    @JsonProperty("workflow_runs") List<GWorkflowRun> workflowRuns)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GWorkflowRun(
    @JsonProperty("id") BigInteger id,
    @JsonProperty("name") String name,
    @JsonProperty("conclusion") String conclusion,
    @JsonProperty("status") String status)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GPullRequest(
    @JsonProperty("id") BigInteger id,
    @JsonProperty("number") BigInteger number,
    @JsonProperty("user") GUser user,
    @JsonProperty("title") String title,
    @JsonProperty("mergeable") boolean mergeable,
    @JsonProperty("mergeable_state") String mergeableState,
    @JsonProperty("draft") boolean draft,
    @JsonProperty("statuses_url") URI statusesURL,
    @JsonProperty("commits_url") URI commitsURL)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GUser(
    @JsonProperty("name") String name,
    @JsonProperty("login") String login,
    @JsonProperty("id") BigInteger id)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GCommit(
    @JsonProperty("sha") String sha,
    @JsonProperty("author") GUser author,
    @JsonProperty("commit") GCommitRaw commit)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GCommitRaw(
    @JsonProperty("verification") GVerification verification)
  {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GVerification(
    @JsonProperty("verified") boolean verified,
    @JsonProperty("reason") String reason,
    @JsonProperty("signature") String signature,
    @JsonProperty("payload") String payload,
    @JsonProperty("verified_at") String verified_at)
  {

  }
}
