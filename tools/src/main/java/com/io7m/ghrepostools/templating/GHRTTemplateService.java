/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

import static freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX;

/**
 * A service supplying freemarker templates.
 */

public final class GHRTTemplateService
{
  private final Configuration configuration;

  private GHRTTemplateService(
    final Configuration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  /**
   * @return A service supplying freemarker templates.
   */

  public static GHRTTemplateService create()
  {
    final Configuration configuration =
      new Configuration(Configuration.VERSION_2_3_31);

    configuration.setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setTemplateLoader(new GHRTTemplateLoader());
    return new GHRTTemplateService(configuration);
  }

  private Template findTemplate(
    final String name)
  {
    Objects.requireNonNull(name, "name");
    try {
      return this.configuration.getTemplate(name);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public GHRTTemplateType<GHRTWorkflowModel> workflowMain()
  {
    return new GenericTemplate<>(
      this.findTemplate("workflowMain")
    );
  }

  public GHRTTemplateType<GHRTWorkflowModel> workflowReduced()
  {
    return new GenericTemplate<>(
      this.findTemplate("workflowReduced")
    );
  }

  public GHRTTemplateType<GHRTWorkflowModel> deployMain()
  {
    return new GenericTemplate<>(
      this.findTemplate("deployMain")
    );
  }

  public GHRTTemplateType<GHRTReadmeModel> readme()
  {
    return new GenericTemplate<>(
      this.findTemplate("readme")
    );
  }

  public GHRTTemplateType<GHRTReadmeOpamModel> readmeOpam()
  {
    return new GenericTemplate<>(
      this.findTemplate("readmeOpam")
    );
  }

  @Override
  public String toString()
  {
    return "[GHRTTemplateService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  private static final class GenericTemplate<T extends GHRTTemplateDataModelType>
    implements GHRTTemplateType<T>
  {
    private final Template template;

    GenericTemplate(
      final Template inTemplate)
    {
      this.template = Objects.requireNonNull(inTemplate, "template");
    }

    @Override
    public void process(
      final T value,
      final Writer output)
      throws TemplateException, IOException
    {
      this.template.process(value.toTemplateHash(), output);
    }
  }
}
