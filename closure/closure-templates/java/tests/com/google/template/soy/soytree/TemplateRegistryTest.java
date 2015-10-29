/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.soytree;

import static com.google.common.truth.Truth.assertThat;
import static com.google.template.soy.soytree.TemplateRegistrySubject.assertThatRegistry;

import com.google.common.collect.Iterables;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.internal.SoyFileKind;
import com.google.template.soy.base.internal.SoyFileSupplier;
import com.google.template.soy.error.FormattingErrorReporter;

import junit.framework.TestCase;

/**
 * Tests for {@link TemplateRegistry}.
 *
 * @author brndn@google.com (Brendan Linn)
 */
public final class TemplateRegistryTest extends TestCase {
  public void testSimple() {
    TemplateRegistry registry =
        SoyFileSetParserBuilder.forSuppliers(
                SoyFileSupplier.Factory.create(
                    "{namespace ns}\n"
                        + "/** Simple template. */\n"
                        + "{template .foo}\n"
                        + "{/template}\n"
                        + "/** Simple deltemplate. */\n"
                        + "{deltemplate bar.baz}\n"
                        + "{/deltemplate}",
                    SoyFileKind.SRC,
                    "example.soy"))
            .parse()
            .registry();
    assertThatRegistry(registry)
        .containsBasicTemplate("ns.foo")
        .definedAt(new SourceLocation("example.soy", 3, 1, 4, 11));
    assertThatRegistry(registry).doesNotContainBasicTemplate("foo");
    assertThatRegistry(registry).containsDelTemplate("bar.baz")
        .definedAt(new SourceLocation("example.soy", 6, 1, 7, 14));
    assertThatRegistry(registry).doesNotContainDelTemplate("ns.bar.baz");
  }

  public void testBasicTemplatesWithSameNamesInDifferentFiles() {
    TemplateRegistry registry =
        SoyFileSetParserBuilder.forSuppliers(
                SoyFileSupplier.Factory.create(
                    "{namespace ns}\n"
                        + "/** Template. */\n"
                        + "{template .foo}\n"
                        + "{/template}\n",
                    SoyFileKind.SRC,
                    "bar.soy"),
                SoyFileSupplier.Factory.create(
                    "{namespace ns2}\n"
                        + "/** Template. */\n"
                        + "{template .foo}\n"
                        + "{/template}\n",
                    SoyFileKind.SRC,
                    "baz.soy"))
            .parse()
            .registry();

    assertThatRegistry(registry).containsBasicTemplate("ns.foo")
        .definedAt(new SourceLocation("bar.soy", 3, 1, 4, 11));
    assertThatRegistry(registry).containsBasicTemplate("ns2.foo")
        .definedAt(new SourceLocation("baz.soy", 3, 1, 4, 11));
  }

  public void testDelTemplates() {
    TemplateRegistry registry =
        SoyFileSetParserBuilder.forSuppliers(
                SoyFileSupplier.Factory.create(
                    "{namespace ns}\n"
                        + "/** Deltemplate. */\n"
                        + "{deltemplate foo.bar}\n"
                        + "{/deltemplate}",
                    SoyFileKind.SRC,
                    "foo.soy"),
                SoyFileSupplier.Factory.create(
                    "{delpackage foo}\n"
                        + "{namespace ns}\n"
                        + "/** Deltemplate. */\n"
                        + "{deltemplate foo.bar}\n"
                        + "{/deltemplate}",
                    SoyFileKind.SRC,
                    "bar.soy"))
            .parse()
            .registry();


    assertThatRegistry(registry).containsDelTemplate("foo.bar")
        .definedAt(new SourceLocation("foo.soy", 3, 1, 4, 14));
    assertThatRegistry(registry).containsDelTemplate("foo.bar")
        .definedAt(new SourceLocation("bar.soy", 4, 1, 5, 14));
  }

  public void testDuplicateBasicTemplates() {
    String file = "{namespace ns}\n"
        + "/** Foo. */\n"
        + "{template .foo}\n"
        + "{/template}\n"
        + "/** Foo. */\n"
        + "{template .foo}\n"
        + "{/template}\n";
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    SoyFileSetParserBuilder.forFileContents(file).errorReporter(errorReporter).parse();
    assertThat(errorReporter.getErrorMessages()).hasSize(1);
    assertThat(Iterables.getOnlyElement(errorReporter.getErrorMessages())).contains(
        "Template 'ns.foo' already defined at no-path:3:1");
  }

  public void testDuplicateDefaultDeltemplates() {
    String file = "{namespace ns}\n"
        + "/** Foo. */\n"
        + "{deltemplate foo.bar}\n"
        + "{/deltemplate}\n"
        + "/** Foo. */\n"
        + "{deltemplate foo.bar}\n"
        + "{/deltemplate}\n";
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    SoyFileSetParserBuilder.forFileContents(file).errorReporter(errorReporter).parse();
    assertThat(errorReporter.getErrorMessages()).hasSize(1);
    assertThat(Iterables.getOnlyElement(errorReporter.getErrorMessages())).contains(
        "Delegate template 'foo.bar' already has a default defined at no-path:3:1");
  }

  public void testDuplicateDeltemplatesInSameDelpackage() {
    String file = "{delpackage foo}\n"
        + "{namespace ns}\n"
        + "/** Foo. */\n"
        + "{deltemplate foo.bar}\n"
        + "{/deltemplate}\n"
        + "/** Foo. */\n"
        + "{deltemplate foo.bar}\n"
        + "{/deltemplate}\n";
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    SoyFileSetParserBuilder.forFileContents(file).errorReporter(errorReporter).parse();
    assertThat(errorReporter.getErrorMessages()).hasSize(1);
    assertThat(Iterables.getOnlyElement(errorReporter.getErrorMessages())).contains(
        "Delegate template 'foo.bar' already defined in delpackage foo: no-path:4:1");
  }
}
