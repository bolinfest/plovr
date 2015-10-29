/*
 * Copyright 2010 Google Inc.
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

package com.google.template.soy.passes;

import static com.google.common.truth.Truth.assertThat;

import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.passes.ClearSoyDocStringsVisitor;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.TemplateNode;

import junit.framework.TestCase;

/**
 * Unit tests for ClearSoyDocStringsVisitor.
 *
 */
public final class ClearSoyDocStringsVisitorTest extends TestCase {


  public void testClearSoyDocStrings() {

    String testFileContent =
        "{namespace boo autoescape=\"deprecated-noncontextual\"}\n" +
        "\n" +
        "/**\n" +
        " * blah blah blah\n" +
        " *\n" +
        " * @param goo blah blah\n" +
        " */\n" +
        "{template .foo}\n" +
        "  {$goo}\n" +
        "{/template}\n";

    ErrorReporter boom = ExplodingErrorReporter.get();
    SoyFileSetNode soyTree =
        SoyFileSetParserBuilder.forFileContents(testFileContent)
            .errorReporter(boom)
            .parse()
            .fileSet();
    TemplateNode template = (TemplateNode) SharedTestUtils.getNode(soyTree);

    assertThat(template.getSoyDoc()).contains("blah");
    assertThat(template.getSoyDocDesc()).contains("blah");
    assertThat(template.getParams().get(0).desc()).contains("blah");

    new ClearSoyDocStringsVisitor().exec(soyTree);

    assertThat(template.getSoyDoc()).isNull();
    assertThat(template.getSoyDocDesc()).isNull();
    assertThat(template.getParams().get(0).desc()).isNull();
  }

}
