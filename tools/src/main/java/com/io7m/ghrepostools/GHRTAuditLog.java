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

package com.io7m.ghrepostools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public final class GHRTAuditLog
  implements AutoCloseable
{
  private final OutputStream output;
  private final JsonMapper mapper;
  private final Object writeLock;

  private GHRTAuditLog(
    final OutputStream inOutput,
    final JsonMapper inMapper)
  {
    this.output = inOutput;
    this.mapper = inMapper;
    this.writeLock = new Object();
  }

  public static GHRTAuditLog open(
    final Path file)
    throws IOException
  {
    final var output =
      Files.newOutputStream(file, CREATE, APPEND, WRITE);
    final var mapper =
      JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    return new GHRTAuditLog(output, mapper);
  }

  public void write(
    final AuditEvent event)
    throws IOException
  {
    synchronized (this.writeLock) {
      this.output.write(this.mapper.writeValueAsBytes(event));
      this.output.write('\n');
      this.output.flush();
    }
  }

  public record AuditEvent(
    @JsonProperty(value = "Type", required = true)
    String type,
    @JsonProperty(value = "Time", required = true)
    OffsetDateTime time,
    @JsonProperty(value = "Data", required = true)
    Map<String, String> data)
  {

  }

  @Override
  public synchronized void close()
    throws Exception
  {
    synchronized (this.writeLock) {
      this.output.close();
    }
  }
}
