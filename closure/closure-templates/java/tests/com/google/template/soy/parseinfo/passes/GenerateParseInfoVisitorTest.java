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


package com.google.template.soy.parseinfo.passes;

import static com.google.template.soy.parseinfo.passes.GenerateParseInfoVisitor.JavaClassNameSource.GENERIC;
import static com.google.template.soy.parseinfo.passes.GenerateParseInfoVisitor.JavaClassNameSource.SOY_FILE_NAME;
import static com.google.template.soy.parseinfo.passes.GenerateParseInfoVisitor.JavaClassNameSource.SOY_NAMESPACE_LAST_PART;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.internal.IndentedLinesBuilder;
import com.google.template.soy.base.internal.SoyFileKind;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.soytree.AliasDeclaration;
import com.google.template.soy.soytree.NameAttributePair;
import com.google.template.soy.soytree.NamespaceDeclaration;
import com.google.template.soy.soytree.SoyFileNode;

import junit.framework.TestCase;

/**
 * Unit tests for GenerateParseInfoVisitor.
 *
 * <p> Note: Testing of the actual code generation happens in {@code SoyFileSetTest}.
 *
 */
public final class GenerateParseInfoVisitorTest extends TestCase {

  public void testJavaClassNameSource() {
    SoyFileNode soyFileNode = forFilePathAndNamespace("BooFoo.soy", "aaa.bbb.cccDdd");
    assertEquals("BooFoo", SOY_FILE_NAME.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("blah/bleh/boo_foo.soy", "aaa.bbb.cccDdd");
    assertEquals("BooFoo", SOY_FILE_NAME.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("boo-FOO.soy", "aaa.bbb.cccDdd");
    assertEquals("BooFoo", SOY_FILE_NAME.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("\\BLAH\\BOO_FOO.SOY", "aaa.bbb.cccDdd");
    assertEquals("BooFoo", SOY_FILE_NAME.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("", "cccDdd");
    assertEquals("CccDdd", SOY_NAMESPACE_LAST_PART.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("", "aaa.bbb.cccDdd");
    assertEquals("CccDdd", SOY_NAMESPACE_LAST_PART.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("", "aaa_bbb.ccc_ddd");
    assertEquals("CccDdd", SOY_NAMESPACE_LAST_PART.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("", "CccDdd");
    assertEquals("CccDdd", SOY_NAMESPACE_LAST_PART.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("", "aaa.bbb.ccc_DDD");
    assertEquals("CccDdd", SOY_NAMESPACE_LAST_PART.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("BooFoo.soy", "aaa.bbb.cccDdd");
    assertEquals("File", GENERIC.generateBaseClassName(soyFileNode));

    soyFileNode = forFilePathAndNamespace("blah/bleh/boo-foo.soy", "ccc_ddd");
    assertEquals("File", GENERIC.generateBaseClassName(soyFileNode));
  }


  public void testAppendJavadoc() {

    String doc = "" +
        "Blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah" +
        " blah blah blah blah blah blah blahblahblahblahblahblahblahblahblahblahblahblahblah" +
        "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah.";
    String expectedJavadoc = "" +
        "    /**\n" +
        "     * Blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah" +
        " blah blah\n" +
        "     * blah blah blah blah blah\n" +
        "     * blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah" +
        "blahblahb\n" +
        "     * lahblahblahblahblahblahblahblahblahblahblahblah.\n" +
        "     */\n";
    IndentedLinesBuilder ilb = new IndentedLinesBuilder(2, 4);
    GenerateParseInfoVisitor.appendJavadoc(ilb, doc, false, true);
    assertEquals(expectedJavadoc, ilb.toString());
  }

  private static SoyFileNode forFilePathAndNamespace(String filePath, String namespace) {
    return new SoyFileNode(
        0,
        filePath,
        SoyFileKind.SRC,
        ExplodingErrorReporter.get(),
        null /* delpackageName */,
        new NamespaceDeclaration(
            namespace, ImmutableList.<NameAttributePair>of(), ExplodingErrorReporter.get()),
        ImmutableList.<AliasDeclaration>of());
  }
}
