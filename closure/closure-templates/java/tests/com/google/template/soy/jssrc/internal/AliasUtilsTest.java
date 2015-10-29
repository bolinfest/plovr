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

package com.google.template.soy.jssrc.internal;

import static com.google.common.truth.Truth.assertThat;

import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.soytree.SoyFileSetNode;

import junit.framework.TestCase;

public class AliasUtilsTest extends TestCase {
  public void testLocalFunctionAliasing() {
    String fileBody = ""
        + "{namespace foo.bar.baz}"
        + "/** */"
        + "{template .localOne}{/template}\n";

    SoyFileSetNode n = SoyFileSetParserBuilder.forFileContents(fileBody).parse().fileSet();
    TemplateAliases templateAliases = AliasUtils.createTemplateAliases(n.getChild(0));
    
    String alias = templateAliases.get("foo.bar.baz.localOne");
    assertThat(alias).isEqualTo("$localOne");
    assertThat(AliasUtils.isExternalFunction(alias)).isFalse();
  }
  
  /*
   * Tests that a local template's name is aliased correctly when a call is encountered first. 
   */
  public void testLocalFunctionCallAliasing() {
    String fileBody = ""
        + "{namespace foo.bar.baz}"
        + "/** */"
        + "{template .localOne}{call .localTwo /}{/template}\n"
        + "{template .localTwo}{/template}\n";


    SoyFileSetNode n = SoyFileSetParserBuilder.forFileContents(fileBody).parse().fileSet();
    TemplateAliases templateAliases = AliasUtils.createTemplateAliases(n.getChild(0));
    
    String alias = templateAliases.get("foo.bar.baz.localTwo");
    assertThat(alias).isEqualTo("$localTwo");
    assertThat(AliasUtils.isExternalFunction(alias)).isFalse();
  }
  
  public void testNonLocalFunctionAliasing() {
    String fileBody = ""
        + "{namespace foo.bar.baz}"
        + "/** */"
        + "{template .bam}\n"
        + "  {call other.name.space.bam /}\n"
        + "{/template}\n";

    SoyFileSetNode n = SoyFileSetParserBuilder.forFileContents(fileBody).parse().fileSet();
    TemplateAliases templateAliases = AliasUtils.createTemplateAliases(n.getChild(0));
    
    String alias = templateAliases.get("other.name.space.bam");
    assertThat(alias).isEqualTo("$templateAlias1");
    assertThat(AliasUtils.isExternalFunction(alias)).isTrue();
  }
  
  /*
   * Tests that a template that is called multiple times does not create a new alias.
   */
  public void testMultipleCallAliasing() {
    String fileBody = ""
        + "{namespace foo.bar.baz}"
        + "/** */"
        + "{template .bam}\n"
        + "  {call other.name.space.bam /}\n"
        + "  {call other.name.space.bam /}\n"
        + "{/template}\n";

    SoyFileSetNode n = SoyFileSetParserBuilder.forFileContents(fileBody).parse().fileSet();
    TemplateAliases templateAliases = AliasUtils.createTemplateAliases(n.getChild(0));
    
    String alias = templateAliases.get("other.name.space.bam");
    assertThat(alias).isEqualTo("$templateAlias1");
    assertThat(AliasUtils.isExternalFunction(alias)).isTrue();
  }
}
