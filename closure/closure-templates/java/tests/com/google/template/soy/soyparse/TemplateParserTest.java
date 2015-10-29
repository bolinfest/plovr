/*
 * Copyright 2008 Google Inc.
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

package com.google.template.soy.soyparse;

import static com.google.common.truth.Truth.assertThat;

import com.google.template.soy.base.internal.IncrementingIdGenerator;
import com.google.template.soy.basetree.SyntaxVersion;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.error.FormattingErrorReporter;
import com.google.template.soy.exprtree.FieldAccessNode;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.IntegerNode;
import com.google.template.soy.exprtree.OperatorNodes.GreaterThanOpNode;
import com.google.template.soy.exprtree.OperatorNodes.NegativeOpNode;
import com.google.template.soy.exprtree.OperatorNodes.PlusOpNode;
import com.google.template.soy.exprtree.StringNode;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.soytree.CallBasicNode;
import com.google.template.soy.soytree.CallDelegateNode;
import com.google.template.soy.soytree.CallNode;
import com.google.template.soy.soytree.CallParamContentNode;
import com.google.template.soy.soytree.CallParamValueNode;
import com.google.template.soy.soytree.CssNode;
import com.google.template.soy.soytree.DebuggerNode;
import com.google.template.soy.soytree.ForNode;
import com.google.template.soy.soytree.ForeachIfemptyNode;
import com.google.template.soy.soytree.ForeachNode;
import com.google.template.soy.soytree.ForeachNonemptyNode;
import com.google.template.soy.soytree.IfCondNode;
import com.google.template.soy.soytree.IfElseNode;
import com.google.template.soy.soytree.IfNode;
import com.google.template.soy.soytree.LetContentNode;
import com.google.template.soy.soytree.LetValueNode;
import com.google.template.soy.soytree.LogNode;
import com.google.template.soy.soytree.MsgFallbackGroupNode;
import com.google.template.soy.soytree.MsgHtmlTagNode;
import com.google.template.soy.soytree.MsgNode;
import com.google.template.soy.soytree.MsgPlaceholderNode;
import com.google.template.soy.soytree.MsgPluralCaseNode;
import com.google.template.soy.soytree.MsgPluralDefaultNode;
import com.google.template.soy.soytree.MsgPluralNode;
import com.google.template.soy.soytree.MsgSelectCaseNode;
import com.google.template.soy.soytree.MsgSelectDefaultNode;
import com.google.template.soy.soytree.MsgSelectNode;
import com.google.template.soy.soytree.PrintDirectiveNode;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.RawTextNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SwitchCaseNode;
import com.google.template.soy.soytree.SwitchDefaultNode;
import com.google.template.soy.soytree.SwitchNode;
import com.google.template.soy.soytree.TemplateNodeBuilder.DeclInfo;
import com.google.template.soy.soytree.TemplateNodeBuilder.DeclInfo.Type;
import com.google.template.soy.soytree.TemplateSubject;
import com.google.template.soy.soytree.XidNode;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.List;

/**
 * Unit tests for the template parser.
 *
 */
public final class TemplateParserTest extends TestCase {

  private static final ErrorReporter FAIL = ExplodingErrorReporter.get();

  public void testMaybeWhitespaceEmitsLineNumberOnError() {

    // Check that the line number is included.
    try {
      parseMaybeWhitespace("ErrorMessage", "foo");
      fail("Should have failed with a ParseException");
    } catch (ParseException pe) {
      assertThat(pe.getMessage()).contains("Found at: test.soy:1:1");
    }

    // The line number is correct following a newline.
    try {
      parseMaybeWhitespace("ErrorMessage", "\nfoo");
      fail("Should have failed with a ParseException");
    } catch (ParseException pe) {
      assertThat(pe.getMessage()).contains("Found at: test.soy:2:1");
    }

    // The line number is correct even when the template doesn't start on line 1.
    try {
      new TemplateParser(
              new IncrementingIdGenerator(),
              "foo",
              "test.soy",
              2, // start line number
              0, // start col number
              ExplodingErrorReporter.get())
          .MaybeWhitespace("ErrorMessage");
      fail("Should have failed with a ParseException");
    } catch (ParseException pe) {
      assertThat(pe.getMessage()).contains("Found at: test.soy:2:1");
    }
  }


  // -----------------------------------------------------------------------------------------------
  // Tests for recognition only.


  public void testRecognizeSoyTag() throws Exception {

    assertIsTemplateBody("{sp}");
    assertIsTemplateBody("{space}");
    assertIsTemplateBody("{ sp }");
    assertIsTemplateBody("{{sp}}");
    assertIsTemplateBody("{{space}}");
    assertIsTemplateBody("{{ {sp} }}");
    assertIsTemplateBody("{{ {} }}");
    assertIsTemplateBody("{{ }s{p  { }}");

    assertIsNotTemplateBody("{}");
    assertIsNotTemplateBody("{sp");
    assertIsNotTemplateBody("{sp blah}");
    assertIsNotTemplateBody("{print { }");
    assertIsNotTemplateBody("{print } }");
    assertIsNotTemplateBody("{print }}");
    assertIsNotTemplateBody("{{}}");
    assertIsNotTemplateBody("{{{blah: blah}}}");
    assertIsNotTemplateBody("blah}blah");
    assertIsNotTemplateBody("blah}}blah");
    assertIsNotTemplateBody("{{print {{ }}");
    assertIsNotTemplateBody("{{print {}}");
  }


  public void testRecognizeRawText() throws Exception {
    assertIsTemplateBody("blah>blah<blah<blah>blah>blah>blah>blah<blah");
    assertIsTemplateBody("{sp}{nil}{\\n}{{\\r}}{\\t}{lb}{{rb}}");
    assertIsTemplateBody(
        "blah{literal}{ {{{ } }{ {}} { }}}}}}}\n" +
            "}}}}}}}}}{ { {{/literal}blah");

    assertIsNotTemplateBody("{sp ace}");
    assertIsNotTemplateBody("{/literal}");
    assertIsNotTemplateBody("{literal attr=\"value\"}");
    assertIsNotTemplateBody("{literal}{literal}{/literal}");
  }

  public void testRecognizeComments() throws Exception {
    assertIsTemplateBody("" +
        "blah // }\n" +
        "{$boo}{msg desc=\"\"} //}\n" +
        "{/msg} // {/msg}\n" +
        "{foreach $item in $items}\t// }\n" +
        "{$item.name}{/foreach} //{{{{\n");

    assertIsTemplateBody("" +
        "blah /* } */\n" +
        "{msg desc=\"\"} /*}*/{$boo}\n" +
        "/******************/ {/msg}\n" +
        "/* {}} { }* }* / }/ * { **}  //}{ { } {\n" +
        "\n  } {//*} {* /} { /* /}{} {}/ } **}}} */\n" +
        "{foreach $item in $items} /* }\n" +
        "{{{{{*/{$item.name}{/foreach}/*{{{{*/\n");

    assertIsTemplateBody("" +
        "blah /** } */\n" +
        "{msg desc=\"\"} /**}*/{$boo}\n" +
        "/******************/ {/msg}\n" +
        "/** {}} { }* }* / }/ * { **}  //}{ { } {\n" +
        "\n  } {//**} {* /} { /** /}{} {}/ } **}}} */\n" +
        "{foreach $item in $items} /** }\n" +
        "{{{{{*/{$item.name}{/foreach}/**{{{{*/\n");

    assertIsTemplateBody("//}\n");
    assertIsTemplateBody(" //}\n");
    assertIsTemplateBody("\n//}\n");
    assertIsTemplateBody("\n //}\n");

    assertIsTemplateBody("/*}*/\n");
    assertIsTemplateBody(" /*}*/\n");
    assertIsTemplateBody("\n/*}\n}*/\n");
    assertIsTemplateBody("\n /*}\n*/\n");

    assertIsTemplateBody("/**}*/\n");
    assertIsTemplateBody(" /**}*/\n");
    assertIsTemplateBody("\n/**}\n}*/\n");
    assertIsTemplateBody("\n /**}\n*/\n");

    assertIsNotTemplateBody("{blah /* { */ blah}");
    assertIsNotTemplateBody("{foreach $item // }\n" +
                            "         in $items}\n" +
                            "{$item}{/foreach}\n");
    assertIsNotTemplateBody("aa////}\n");
    assertIsNotTemplateBody("{nil}//}\n");
  }

  public void testRecognizeHeaderParams() throws Exception {
    assertIsTemplateContent("{@param ...}\n");
    assertIsTemplateContent("{@param ...}\nBODY");
    assertIsTemplateContent("  {@param ...}\n  BODY");
    assertIsTemplateContent("\n{@param ...}\n");
    assertIsTemplateContent("  \n{@param ...}\nBODY");
    assertIsTemplateContent("  \n  {@param ...\n  ...}\n  BODY");

    assertIsTemplateContent("" +
        "  {@param ...}  {@param ...}\n" +
        "  {@param ...}  /** ... */\n" +  // doc comment
        "  {@param ...}  // ...\n" +  // nondoc comment
        "  {@param ...\n" +
        "      ...}  /** ...\n" +  // doc comment
        "      ...\n" +
        "      ... */\n" +
        "  /*\n" +  // nondoc comment
        "   * ...\n" +
        "   */\n" +
        "  /* ... */\n" +  // nondoc comment
        "  {@param ...}  /**\n" +  // doc comment
        "      ... */  \n" +
        "  {@param ...}  /*\n" +  // nondoc comment
        "      ... */  \n" +
        "\n" +
        "  BODY\n");

    assertIsTemplateContent("" +
        "  /** */{@param ...}\n" +  // doc comment
        "  /** \n" +  // doc comment
        "   */{@param ...}\n" +
        "\n" +
        "  BODY\n");

    assertIsNotTemplateContent("{@param ...}");
    assertIsNotTemplateContent("{@ param ...}\n");
    assertIsNotTemplateContent("{@foo}\n");
    assertIsNotTemplateContent("{@foo ...}\n");

    assertIsTemplateContent("" +
        "  /** ... */\n" +  // doc comment
        "  {@param ...}\n" +
        "  BODY\n");
    assertIsTemplateContent("" +
        "  {@param ...}\n" +
        "  /**\n" +  // doc comment
        "   * ...\n" +
        "   */\n" +
        "  {@param ...}\n" +
        "  BODY\n");
    assertIsTemplateContent("" +
        "  {@param ...}  /*\n" +
        "      */  /** ... */\n" +  // doc comment
        "  {@param ...}\n" +
        "  BODY\n");
  }

  public void testRecognizeHeaderInjectedParams() throws Exception {
    assertIsTemplateContent("{@inject ...}\n");
    assertIsTemplateContent("{@inject ...}\nBODY");
    assertIsTemplateContent("  {@inject ...}\n  BODY");
    assertIsTemplateContent("\n{@inject ...}\n");
    assertIsTemplateContent("  \n{@inject ...}\nBODY");
    assertIsTemplateContent("  \n  {@inject ...\n  ...}\n  BODY");

    assertIsTemplateContent("" +
        "  {@inject ...}  {@inject ...}\n" +
        "  {@inject ...}  /** ... */\n" +  // doc comment
        "  {@inject ...}  // ...\n" +  // nondoc comment
        "  {@inject ...\n" +
        "      ...}  /** ...\n" +  // doc comment
        "      ...\n" +
        "      ... */\n" +
        "  /*\n" +  // nondoc comment
        "   * ...\n" +
        "   */\n" +
        "  /* ... */\n" +  // nondoc comment
        "  {@inject ...}  /**\n" +  // doc comment
        "      ... */  \n" +
        "  {@inject ...}  /*\n" +  // nondoc comment
        "      ... */  \n" +
        "\n" +
        "  BODY\n");

    assertIsTemplateContent("" +
        "  /** */{@inject ...}\n" +  // doc comment
        "  /** \n" +  // doc comment
        "   */{@inject ...}\n" +
        "\n" +
        "  BODY\n");

    assertIsNotTemplateContent("{@inject ...}");
    assertIsNotTemplateContent("{@ param ...}\n");
    assertIsNotTemplateContent("{@foo}\n");
    assertIsNotTemplateContent("{@foo ...}\n");

    assertIsTemplateContent("" +
        "  /** ... */\n" +  // doc comment
        "  {@inject ...}\n" +
        "  BODY\n");
    assertIsTemplateContent("" +
        "  {@inject ...}\n" +
        "  /**\n" +  // doc comment
        "   * ...\n" +
        "   */\n" +
        "  {@inject ...}\n" +
        "  BODY\n");
    assertIsTemplateContent("" +
        "  {@inject ...}  /*\n" +
        "      */  /** ... */\n" +  // doc comment
        "  {@inject ...}\n" +
        "  BODY\n");
  }

  public void testRecognizeCommands() throws Exception {
    assertIsTemplateBody("" +
        "{msg desc=\"blah\" hidden=\"true\"}\n" +
        "  {$boo} is a <a href=\"{$fooUrl}\">{$foo}</a>.\n" +
        "{/msg}");
    assertIsTemplateBody("" +
        "{msg meaning=\"verb\" desc=\"\"}\n" +
        "  Archive\n" +
        "{fallbackmsg desc=\"\"}\n" +
        "  Archive\n" +
        "{/msg}");
    assertIsTemplateBody("{$aaa + 1}{print $bbb.ccc[$ddd] |noescape}");
    assertIsTemplateBody("{css selected-option}{css CSS_SELECTED_OPTION}{css $cssSelectedOption}");
    assertIsTemplateBody("{xid selected-option}{xid SELECTED_OPTION_ID}");
    assertIsTemplateBody("{if $boo}foo{elseif $goo}moo{else}zoo{/if}");
    assertIsTemplateBody("" +
        "  {switch $boo}\n" +
        "    {case $foo} blah blah\n" +
        "    {case 2, $goo.moo, 'too'} bleh bleh\n" +
        "    {default} bluh bluh\n" +
        "  {/switch}\n");
    assertIsTemplateBody("{foreach $item in $items}{index($item)}. {$item.name}<br>{/foreach}");
    assertIsTemplateBody("" +
        "{for $i in range($boo + 1,\n" +
        "                 88, 11)}\n" +
        "Number {$i}.{{/for}}");
    assertIsTemplateBody("{call aaa.bbb.ccc data=\"all\" /}");
    assertIsTemplateBody("" +
        "{call .aaa}\n" +
        "  {{param boo: $boo /}}\n" +
        "  {param foo}blah blah{/param}\n" +
        "  {param foo kind=\"html\"}blah blah{/param}\n" +
        "{/call}");
    assertIsTemplateBody(
        "{call .aaa}\n" +
        "  {param foo : bar \" baz/}\n" +
        "{/call}\n");
    assertIsTemplateBody("{call aaa.bbb.ccc data=\"all\" /}");
    assertIsTemplateBody("" +
        "{call .aaa}\n" +
        "  {{param boo: $boo /}}\n" +
        "  {param foo}blah blah{/param}\n" +
        "{/call}");
    assertIsTemplateBody("{delcall aaa.bbb.ccc data=\"all\" /}");
    assertIsTemplateBody("" +
        "{delcall ddd.eee}\n" +
        "  {{param boo: $boo /}}\n" +
        "  {param foo}blah blah{/param}\n" +
        "{/delcall}");
    assertIsTemplateBody("" +
        "{msg meaning=\"boo\" desc=\"blah\"}\n" +
        "  {$boo phname=\"foo\"} is a \n" +
        "  <a phname=\"begin_link\" href=\"{$fooUrl}\">\n" +
        "    {$foo |noAutoescape phname=\"booFoo\" }\n" +
        "  </a phname=\"END_LINK\" >.\n" +
        "  {call .aaa data=\"all\"\nphname=\"AaaBbb\"/}\n" +
        "  {call .aaa phname=\"AaaBbb\" data=\"all\"}{/call}\n" +
        "{/msg}");
    assertIsTemplateBody("{log}Blah blah.{/log}");
    assertIsTemplateBody("{debugger}");
    assertIsTemplateBody("{let $foo : 1 + 2/}\n");
    assertIsTemplateBody("{let $foo : '\"'/}\n");
    assertIsTemplateBody("{let $foo}Hello{/let}\n");
    assertIsTemplateBody("{let $foo kind=\"html\"}Hello{/let}\n");

    assertIsNotTemplateBody("{namespace}");
    assertIsNotTemplateBody("{template}\n" + "blah\n" + "{/template}\n");
    assertIsNotTemplateBody("{msg}blah{/msg}");
    assertIsNotTemplateBody("{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}<a href=http://www.google.com{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}blah{msg desc=\"\"}bleh{/msg}bluh{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}blah{/msg blah}");
    assertIsNotTemplateBody("{msg}<blah<blah>{/msg}");
    assertIsNotTemplateBody("{msg}blah>blah{/msg}");
    assertIsNotTemplateBody("{msg}<blah>blah>{/msg}");
    assertIsNotTemplateBody("" +
        "{msg meaning=\"verb\" desc=\"\"}\n" +
        "  Archive\n" +
        "{fallbackmsg desc=\"\"}\n" +
        "  Archive\n" +
        "{fallbackmsg desc=\"\"}\n" +
        "  Store\n" +
        "{/msg}");
    assertIsNotTemplateBody("{print $boo /}");
    assertIsNotTemplateBody("{if true}aaa{else/}bbb{/if}");
    assertIsNotTemplateBody("{call .aaa.bbb /}");
    assertIsNotTemplateBody("{delcall ddd.eee}{param foo: 0}{/call}");
    assertIsNotTemplateBody("{delcall .dddEee /}");
    assertIsNotTemplateBody("{call.aaa}{param boo kind=\"html\": 123 /}{/call}\n");
    assertIsNotTemplateBody("{log}");
    assertIsNotTemplateBody("{log 'Blah blah.'}");
    assertIsNotTemplateBody("{let $foo kind=\"html\" : 1 + 1/}\n");
    assertIsNotTemplateBody("{xid a.b-c}");
    assertIsNotTemplateBody("{msg desc=\"\"}{$boo phname=\"boo.foo\"}{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}<br phname=\"boo-foo\" />{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}{call .boo phname=\"boo\" phname=\"boo\" /}{/msg}");
    assertIsNotTemplateBody("{msg desc=\"\"}<br phname=\"break\" phname=\"break\" />{/msg}");
  }


  public void testRecognizeMsgPlural() throws Exception {
    // Normal, valid plural message.
    assertIsTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"1\"}\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

    // Offset is optional.
    assertIsTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people}\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {default}I see {$num_people} in {$place}, including {$person}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

    // Plural message should have a default clause.
    assertIsNotTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"1\"}\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

    // default should be the last clause, after all cases.
    assertIsNotTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"1\"}\n" +
        "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

    // Order is irrelevant for cases.
    assertIsTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"1\"}\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

    // Offset should not be less than 0.
    assertIsNotTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"-1\"}\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case 1}I see {$person} in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");

     // Case should not be less than 0.
    assertIsNotTemplateBody(
        "  {msg desc=\"A sample plural message\"}\n" +
        "    {plural $num_people offset=\"1\"}\n" +
        "      {case 0}I see no one in {$place}.\n" +
        "      {case -1}I see {$person} in {$place}.\n" +
        "      {case 2}I see {$person} and one other person in {$place}.\n" +
        "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "    {/plural}\n" +
        "  {/msg}\n");
  }


  public void testRecognizeMsgSelect() throws Exception {
    assertIsTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // Default should be present.
    assertIsNotTemplateBody(
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n");

    // Default should be the last clause.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {default}{$person} added you to his circle.\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // There is no restriction that 'female' and 'male' should not occur together.
    assertIsTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {case 'male'}{$person} added you to his circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // There is no restriction of case keywords. An arbitrary word like 'neuter' is fine.
    assertIsTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {case 'male'}{$person} added you to his circle.\n" +
        "    {case 'neuter'}{$person} added you to its circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // It is not possible to have more than one string in a case.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $job}\n" +
        "    {case 'hw_engineer', 'sw_engineer'}{$person}, an engineer, liked this.\n" +
        "    {default}{$person} liked this.\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // select should have a default.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {case 'male'}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "{/msg}\n");
  }


  public void testRecognizeNestedPlrsel() throws Exception {
    // Select nested inside select should be allowed.
    assertIsTemplateBody(
        "{msg desc=\"A sample nested message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}\n" +
        "      {select $gender2}\n" +
        "        {case 'female'}{$person1} added {$person2} and her friends to her circle.\n" +
        "        {default}{$person1} added {$person2} and his friends to her circle.\n" +
        "      {/select}\n" +
        "    {default}\n" +
        "      {select $gender2}\n" +
        "        {case 'female'}{$person1} added {$person2} and her friends to his circle.\n" +
        "        {default}{$person1} added {$person2} and his friends to his circle.\n" +
        "      {/select}\n" +
       "  {/select}\n" +
        "{/msg}\n");

    // Plural nested inside select should be allowed.
    assertIsTemplateBody(
        "{msg desc=\"A sample nested message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}\n" +
        "      {plural $num_people}\n" +
        "        {case 1}{$person} added one person to her circle.\n" +
        "        {default}{$person} added {$num_people} to her circle.\n" +
        "      {/plural}\n" +
        "    {default}\n" +
        "      {plural $num_people}\n" +
        "        {case 1}{$person} added one person to his circle.\n" +
        "        {default}{$person} added {$num_people} to his circle.\n" +
        "      {/plural}\n" +
        "  {/select}\n" +
        "{/msg}\n");

    // Plural inside plural should not be allowed.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample nested message\"}\n" +
        "  {plural $n_friends}\n" +
        "    {case 1}\n" +
        "      {plural $n_circles}\n" +
        "        {case 1}You have one friend in one circle.\n" +
        "        {default}You have one friend in {$n_circles} circles.\n" +
        "      {/plural}\n" +
        "    {default}\n" +
        "      {plural $n_circles}\n" +
        "        {case 1}You have {$n_friends} friends in one circle.\n" +
        "        {default}You have {$n_friends} friends in {$n_circles} circles.\n" +
        "      {/plural}\n" +
        "  {/plural}\n" +
        "{/msg}\n");

    // Select inside plural should not be allowed.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample nested message\"}\n" +
        "  {plural $n_friends}\n" +
        "    {case 1}\n" +
        "      {select $gender}\n" +
        "        {case 'female'}{$person} has one person in her circle.\n" +
        "        {default}{$person} has one person in his circle.\n" +
        "      {/select}\n" +
        "    {default}\n" +
        "      {select $gender}\n" +
        "        {case 'female'}{$person} has {$n_friends} persons in her circle.\n" +
        "        {default}{$person} has {$n_friends} persons in his circle.\n" +
        "      {/select}\n" +
        "  {/plural}\n" +
        "{/msg}\n");

    // Messages with more than one plural/gender clauses should not be allowed.
    assertIsNotTemplateBody(
        "{msg desc=\"A sample plural message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "  {plural $num_people offset=\"1\"}\n" +
        "    {case 0}I see no one in {$place}.\n" +
        "    {case 1}I see {$person} in {$place}.\n" +
        "    {case 2}I see {$person} and one other person in {$place}.\n" +
        "    {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
        "  {/plural}\n" +
        " {/msg}\n");

  }


  // -----------------------------------------------------------------------------------------------
  // Tests for recognition and parse results.


  public void testParseRawText() throws Exception {

    String templateBody =
        "  {sp} aaa bbb  \n" +
        "  ccc {lb}{rb} ddd {\\n}\n" +
        "  eee <br>\n" +
        "  fff\n" +
        "  {literal}ggg\n" +
        "hhh }{  {/literal}  \n" +
        "  \u2222\uEEEE\u9EC4\u607A\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());
    RawTextNode rtn = (RawTextNode) nodes.get(0);
    assertEquals(
        "  aaa bbb ccc {} ddd \neee <br>fffggg\nhhh }{  \u2222\uEEEE\u9EC4\u607A",
        rtn.getRawText());
    assertEquals(
        "  aaa bbb ccc {lb}{rb} ddd {\\n}eee <br>fffggg{\\n}hhh {rb}{lb}  \u2222\uEEEE\u9EC4\u607A",
        rtn.toSourceString());
  }


  public void testParseComments() throws Exception {

    String templateBody = "" +
        "  {sp}  // {sp}\n" +  // first {sp} outside of comments
        "  /* {sp} {sp} */  // {sp}\n" +
        "  /** {sp} {sp} */  // {sp}\n" +
        "  /* {sp} */{sp}/* {sp} */\n" +  // middle {sp} outside of comments
        "  /** {sp} */{sp}/** {sp} */\n" +  // middle {sp} outside of comments
        "  /* {sp}\n" +
        "  {sp} */{sp}\n" +  // last {sp} outside of comments
        "  /** {sp}\n" +
        "  {sp} */{sp}\n" +  // last {sp} outside of comments
        "  {sp}/* {sp}\n" +  // first {sp} outside of comments
        "  {sp} */\n" +
        "  {sp}/** {sp}\n" +  // first {sp} outside of comments
        "  {sp} */\n" +
        "  // {sp} /* {sp} */\n" +
        "  // {sp} /** {sp} */\n" +
        "  http://www.google.com\n";  // not a comment if "//" preceded by a non-space such as ":"

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());
    assertEquals("       http://www.google.com", ((RawTextNode) nodes.get(0)).getRawText());
  }


  public void testParseHeaderDecls() throws Exception {

    String templateHeaderAndBody = "" +
        "  {@param boo: string}  // Something scary. (Not doc comment.)\n" +
        "  {@param foo: list<int>}  /** Something random. */\n" +
        "  {@param goo: string}/** Something\n" +
        "      slimy. */\n" +
        "  /* Something strong. (Not doc comment.) */" +
        "  // {@param commentedOut: string}\n" +
        "  {@param moo: string}{@param too: string}\n" +
        "  {@param? woo: string}  /** Something exciting. */  {@param hoo: string}\n" +
        "  BODY\n";

    TemplateParseResult result = parseTemplateContent(templateHeaderAndBody, FAIL);
    assertEquals(7, result.getHeaderDecls().size());
    assertEquals("BODY", result.getBodyNodes().get(0).toSourceString());

    List<DeclInfo> declInfos = result.getHeaderDecls();
    assertEquals(Type.PARAM, declInfos.get(0).type());
    assertEquals("boo: string", declInfos.get(0).cmdText());
    assertEquals(null, declInfos.get(0).soyDoc());
    assertEquals("foo: list<int>", declInfos.get(1).cmdText());
    assertEquals("Something random.", declInfos.get(1).soyDoc());
    assertEquals("Something\n      slimy.", declInfos.get(2).soyDoc());
    assertEquals(null, declInfos.get(3).soyDoc());
    assertEquals(null, declInfos.get(4).soyDoc());
    assertEquals("Something exciting.", declInfos.get(5).soyDoc());
    assertEquals(null, declInfos.get(6).soyDoc());
  }


  public void testParsePrintStmt() throws Exception {

    String templateBody =
        "  {$boo.foo}{$boo.foo}\n" +
        "  {$goo + 1 |noAutoescape}\n" +
        "  {print 'blah    blahblahblah' |escapeHtml|insertWordBreaks:8}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(4, nodes.size());

    PrintNode pn0 = (PrintNode) nodes.get(0);
    assertTrue(pn0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("$boo.foo", pn0.getExprText());
    assertEquals(0, pn0.getChildren().size());
    assertEquals("FOO", pn0.genBasePhName());
    assertEquals("{$boo.foo}", pn0.toSourceString());
    assertTrue(pn0.getExprUnion().getExpr().getRoot() instanceof FieldAccessNode);

    PrintNode pn1 = (PrintNode) nodes.get(1);
    assertTrue(pn0.genSamenessKey().equals(pn1.genSamenessKey()));
    assertTrue(pn1.getExprUnion().getExpr().getRoot() instanceof FieldAccessNode);

    PrintNode pn2 = (PrintNode) nodes.get(2);
    assertTrue(pn2.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("$goo + 1", pn2.getExprText());
    assertEquals(1, pn2.getChildren().size());
    PrintDirectiveNode pn2d0 = pn2.getChild(0);
    assertTrue(pn2d0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("|noAutoescape", pn2d0.getName());
    assertEquals("XXX", pn2.genBasePhName());
    assertTrue(pn2.getExprUnion().getExpr().getRoot() instanceof PlusOpNode);

    PrintNode pn3 = (PrintNode) nodes.get(3);
    assertTrue(pn3.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("'blah    blahblahblah'", pn3.getExprText());
    assertEquals(2, pn3.getChildren().size());
    PrintDirectiveNode pn3d0 = pn3.getChild(0);
    assertTrue(pn3d0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("|escapeHtml", pn3d0.getName());
    PrintDirectiveNode pn3d1 = pn3.getChild(1);
    assertTrue(pn3d1.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("|insertWordBreaks", pn3d1.getName());
    assertEquals(8, ((IntegerNode) pn3d1.getArgs().get(0).getRoot()).getValue());
    assertEquals("XXX", pn3.genBasePhName());
    assertTrue(pn3.getExprUnion().getExpr().getRoot() instanceof StringNode);

    assertFalse(pn0.genSamenessKey().equals(pn2.genSamenessKey()));
    assertFalse(pn3.genSamenessKey().equals(pn0.genSamenessKey()));
  }


  public void testParsePrintStmtWithPhname() throws Exception {

    String templateBody = "" +
        "  {$boo.foo}\n" +
        "  {$boo.foo phname=\"booFoo\"}\n" +
        "  {$boo.foo    phname=\"booFoo\"    }\n" +
        "  {$boo.foo phname=\"boo_foo\"}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(4, nodes.size());

    PrintNode pn0 = (PrintNode) nodes.get(0);
    assertEquals("$boo.foo", pn0.getExprText());
    assertEquals("FOO", pn0.genBasePhName());
    assertEquals("{$boo.foo}", pn0.toSourceString());

    PrintNode pn1 = (PrintNode) nodes.get(1);
    assertEquals("$boo.foo", pn1.getExprText());
    assertEquals("BOO_FOO", pn1.genBasePhName());
    assertEquals("{$boo.foo phname=\"booFoo\"}", pn1.toSourceString());
    assertTrue(pn1.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(0, pn1.getChildren().size());
    assertTrue(pn1.getExprUnion().getExpr().getRoot() instanceof FieldAccessNode);

    PrintNode pn2 = (PrintNode) nodes.get(2);
    assertEquals("$boo.foo", pn2.getExprText());
    assertEquals("BOO_FOO", pn2.genBasePhName());
    assertEquals("{$boo.foo phname=\"booFoo\"}", pn2.toSourceString());

    PrintNode pn3 = (PrintNode) nodes.get(3);
    assertEquals("$boo.foo", pn3.getExprText());
    assertEquals("BOO_FOO", pn3.genBasePhName());
    assertEquals("{$boo.foo phname=\"boo_foo\"}", pn3.toSourceString());

    assertFalse(pn0.genSamenessKey().equals(pn1.genSamenessKey()));
    assertTrue(pn1.genSamenessKey().equals(pn2.genSamenessKey()));
    assertFalse(pn1.genSamenessKey().equals(pn3.genSamenessKey()));
  }


  public void testParseCssStmt() throws Exception {

    String templateBody =
        "{css selected-option}\n" +
        "{css CSS_SELECTED_OPTION}\n" +
        "{css $cssSelectedOption}\n" +
        "{css %SelectedOption}";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(4, nodes.size());
    assertEquals("selected-option", ((CssNode) nodes.get(0)).getCommandText());
    assertEquals("CSS_SELECTED_OPTION", ((CssNode) nodes.get(1)).getCommandText());
    assertEquals("$cssSelectedOption", ((CssNode) nodes.get(2)).getCommandText());
    assertEquals("%SelectedOption", ((CssNode) nodes.get(3)).getCommandText());
  }


  public void testParseXidStmt() throws Exception {

    String templateBody =
        "{xid selected-option}\n" +
        "{xid selected.option}\n" +
        "{xid XID_SELECTED_OPTION}";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(3, nodes.size());
    assertEquals("selected-option", ((XidNode) nodes.get(0)).getText());
    assertEquals("selected.option", ((XidNode) nodes.get(1)).getText());
    assertEquals("XID_SELECTED_OPTION", ((XidNode) nodes.get(2)).getText());
  }


  public void testParseMsgStmt() throws Exception {

    String templateBody =
        "  {msg desc=\"Tells user's quota usage.\"}\n" +
        "    You're currently using {$usedMb} MB of your quota.{sp}\n" +
        "    <a href=\"{$learnMoreUrl}\">Learn more</A>\n" +
        "    <br /><br />\n" +
        "  {/msg}\n" +
        "  {msg meaning=\"noun\" desc=\"\" hidden=\"true\"}Archive{/msg}\n" +
        "  {msg meaning=\"noun\" desc=\"The archive (noun).\"}Archive{/msg}\n" +
        "  {msg meaning=\"verb\" desc=\"\"}Archive{/msg}\n" +
        "  {msg desc=\"\"}Archive{/msg}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(5, nodes.size());

    MsgNode mn0 = ((MsgFallbackGroupNode) nodes.get(0)).getMsg();
    assertEquals("Tells user's quota usage.", mn0.getDesc());
    assertEquals(null, mn0.getMeaning());
    assertEquals(false, mn0.isHidden());
    assertEquals(8, mn0.numChildren());

    assertEquals("You're currently using ", ((RawTextNode) mn0.getChild(0)).getRawText());
    MsgPlaceholderNode mpn1 = (MsgPlaceholderNode) mn0.getChild(1);
    assertEquals("$usedMb", ((PrintNode) mpn1.getChild(0)).getExprText());
    assertEquals(" MB of your quota. ", ((RawTextNode) mn0.getChild(2)).getRawText());

    MsgPlaceholderNode mpn3 = (MsgPlaceholderNode) mn0.getChild(3);
    MsgHtmlTagNode mhtn3 = (MsgHtmlTagNode) mpn3.getChild(0);
    assertEquals("a", mhtn3.getLcTagName());
    assertEquals("START_LINK", mhtn3.genBasePhName());
    assertEquals("<a href=\"{$learnMoreUrl}\">", mhtn3.toSourceString());

    assertEquals(3, mhtn3.numChildren());
    assertEquals("<a href=\"", ((RawTextNode) mhtn3.getChild(0)).getRawText());
    assertEquals("$learnMoreUrl", ((PrintNode) mhtn3.getChild(1)).getExprText());
    assertEquals("\">", ((RawTextNode) mhtn3.getChild(2)).getRawText());

    assertEquals("Learn more", ((RawTextNode) mn0.getChild(4)).getRawText());

    MsgPlaceholderNode mpn5 = (MsgPlaceholderNode) mn0.getChild(5);
    MsgHtmlTagNode mhtn5 = (MsgHtmlTagNode) mpn5.getChild(0);
    assertEquals("/a", mhtn5.getLcTagName());
    assertEquals("END_LINK", mhtn5.genBasePhName());
    assertEquals("</A>", mhtn5.toSourceString());

    MsgPlaceholderNode mpn6 = (MsgPlaceholderNode) mn0.getChild(6);
    MsgHtmlTagNode mhtn6 = (MsgHtmlTagNode) mpn6.getChild(0);
    assertEquals("BREAK", mhtn6.genBasePhName());
    assertTrue(mpn6.shouldUseSameVarNameAs((MsgPlaceholderNode) mn0.getChild(7)));
    assertTrue(! mpn6.shouldUseSameVarNameAs(mpn5));
    assertTrue(! mpn5.shouldUseSameVarNameAs(mpn3));

    MsgFallbackGroupNode mfgn1 = (MsgFallbackGroupNode) nodes.get(1);
    assertEquals(
        "{msg meaning=\"noun\" desc=\"\" hidden=\"true\"}Archive{/msg}", mfgn1.toSourceString());
    MsgNode mn1 = mfgn1.getMsg();
    assertEquals("", mn1.getDesc());
    assertEquals("noun", mn1.getMeaning());
    assertEquals(true, mn1.isHidden());
    assertEquals(1, mn1.numChildren());
    assertEquals("Archive", ((RawTextNode) mn1.getChild(0)).getRawText());
  }


  public void testParseMsgHtmlTagWithPhname() throws Exception {

    String templateBody = "" +
        "  {msg desc=\"\"}\n" +
        "    <a href=\"{$learnMoreUrl}\" phname=\"beginLearnMoreLink\">\n" +
        "      Learn more\n" +
        "    </A phname=\"end_LearnMore_LINK\">\n" +
        "    <br phname=\"breakTag\" /><br phname=\"breakTag\" /><br phname=\"break_tag\" />\n" +
        "  {/msg}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgNode mn0 = ((MsgFallbackGroupNode) nodes.get(0)).getChild(0);
    assertEquals(6, mn0.numChildren());

    MsgPlaceholderNode mpn0 = (MsgPlaceholderNode) mn0.getChild(0);
    MsgHtmlTagNode mhtn0 = (MsgHtmlTagNode) mpn0.getChild(0);
    assertEquals("a", mhtn0.getLcTagName());
    assertEquals("BEGIN_LEARN_MORE_LINK", mhtn0.genBasePhName());
    assertEquals(
        "<a href=\"{$learnMoreUrl}\" phname=\"beginLearnMoreLink\">", mhtn0.toSourceString());

    MsgPlaceholderNode mpn2 = (MsgPlaceholderNode) mn0.getChild(2);
    MsgHtmlTagNode mhtn2 = (MsgHtmlTagNode) mpn2.getChild(0);
    assertEquals("/a", mhtn2.getLcTagName());
    assertEquals("END_LEARN_MORE_LINK", mhtn2.genBasePhName());
    assertEquals("</A phname=\"end_LearnMore_LINK\">", mhtn2.toSourceString());

    MsgPlaceholderNode mpn3 = (MsgPlaceholderNode) mn0.getChild(3);
    MsgHtmlTagNode mhtn3 = (MsgHtmlTagNode) mpn3.getChild(0);
    assertEquals("br", mhtn3.getLcTagName());
    assertEquals("BREAK_TAG", mhtn3.genBasePhName());
    assertEquals("<br  phname=\"breakTag\"/>", mhtn3.toSourceString());

    MsgPlaceholderNode mpn4 = (MsgPlaceholderNode) mn0.getChild(4);
    MsgHtmlTagNode mhtn4 = (MsgHtmlTagNode) mpn4.getChild(0);

    MsgPlaceholderNode mpn5 = (MsgPlaceholderNode) mn0.getChild(5);
    MsgHtmlTagNode mhtn5 = (MsgHtmlTagNode) mpn5.getChild(0);
    assertEquals("br", mhtn5.getLcTagName());
    assertEquals("BREAK_TAG", mhtn5.genBasePhName());
    assertEquals("<br  phname=\"break_tag\"/>", mhtn5.toSourceString());

    assertFalse(mhtn0.genSamenessKey().equals(mhtn2.genSamenessKey()));
    assertFalse(mhtn0.genSamenessKey().equals(mhtn3.genSamenessKey()));
    assertTrue(mhtn3.genSamenessKey().equals(mhtn4.genSamenessKey()));
    assertFalse(mhtn3.genSamenessKey().equals(mhtn5.genSamenessKey()));
  }


  public void testParseMsgStmtWithCall() throws Exception {

    String templateBody =
        "  {msg desc=\"Blah.\"}\n" +
        "    Blah {call .helper_ data=\"all\" /} blah{sp}\n" +
        "    {call .helper_}\n" +
        "      {param foo}Foo{/param}\n" +
        "    {/call}{sp}\n" +
        "    blah.\n" +
        "  {/msg}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgNode mn = ((MsgFallbackGroupNode) nodes.get(0)).getChild(0);
    assertEquals(5, mn.numChildren());

    assertEquals("Blah ", ((RawTextNode) mn.getChild(0)).getRawText());
    assertEquals(0, ((CallNode) ((MsgPlaceholderNode) mn.getChild(1)).getChild(0)).numChildren());
    assertEquals(" blah ", ((RawTextNode) mn.getChild(2)).getRawText());
    assertEquals(1, ((CallNode) ((MsgPlaceholderNode) mn.getChild(3)).getChild(0)).numChildren());
    assertEquals(" blah.", ((RawTextNode) mn.getChild(4)).getRawText());
  }


  public void testParseMsgStmtWithIf() throws Exception {
    TemplateSubject.assertThatTemplateContent(
        "  {msg desc=\"Blah.\"}\n"
            + "    Blah \n"
            + "    {if $boo}\n"
            + "      bleh\n"
            + "    {else}\n"
            + "      bluh\n"
            + "    {/if}\n"
            + "    .\n"
            + "  {/msg}\n")
        .isNotWellFormed();
  }


  public void testParseMsgStmtWithFallback() throws Exception {

    String templateBody = "" +
        "{msg meaning=\"verb\" desc=\"Used as a verb.\"}\n" +
        "  Archive\n" +
        "{fallbackmsg desc=\"\"}\n" +
        "  Archive\n" +
        "{/msg}";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgFallbackGroupNode mfgn = (MsgFallbackGroupNode) nodes.get(0);
    assertEquals(2, mfgn.numChildren());

    MsgNode mn0 = mfgn.getChild(0);
    assertEquals("msg", mn0.getCommandName());
    assertEquals("verb", mn0.getMeaning());
    assertEquals("Used as a verb.", mn0.getDesc());
    assertEquals("Archive", ((RawTextNode) mn0.getChild(0)).getRawText());

    MsgNode mn1 = mfgn.getChild(1);
    assertEquals("fallbackmsg", mn1.getCommandName());
    assertEquals(null, mn1.getMeaning());
    assertEquals("", mn1.getDesc());
    assertEquals("Archive", ((RawTextNode) mn1.getChild(0)).getRawText());
  }


  public void testParseLetStmt() throws Exception {

    String templateBody =
        "  {let $alpha: $boo.foo /}\n" +
        "  {let $beta}Boo!{/let}\n" +
        "  {let $gamma}\n" +
        "    {for $i in range($alpha)}\n" +
        "      {$i}{$beta}\n" +
        "    {/for}\n" +
        "  {/let}\n" +
        "  {let $delta kind=\"html\"}Boo!{/let}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(4, nodes.size());

    LetValueNode alphaNode = (LetValueNode) nodes.get(0);
    assertEquals("alpha", alphaNode.getVarName());
    assertEquals("$boo.foo", alphaNode.getValueExpr().toSourceString());
    LetContentNode betaNode = (LetContentNode) nodes.get(1);
    assertEquals("beta", betaNode.getVarName());
    assertEquals("Boo!", ((RawTextNode) betaNode.getChild(0)).getRawText());
    assertNull(betaNode.getContentKind());
    LetContentNode gammaNode = (LetContentNode) nodes.get(2);
    assertEquals("gamma", gammaNode.getVarName());
    assertTrue(gammaNode.getChild(0) instanceof ForNode);
    assertNull(gammaNode.getContentKind());
    LetContentNode deltaNode = (LetContentNode) nodes.get(3);
    assertEquals("delta", deltaNode.getVarName());
    assertEquals("Boo!", ((RawTextNode) betaNode.getChild(0)).getRawText());
    assertEquals(ContentKind.HTML, deltaNode.getContentKind());

    // Test error case.
    TemplateSubject.assertThatTemplateContent("{let $alpha /}")
        .causesError(LetValueNode.SELF_ENDING_WITHOUT_VALUE)
        .at(1, 1);

    // Test error case.
    TemplateSubject.assertThatTemplateContent("{let $alpha: $boo.foo}")
        .causesError(LetContentNode.NON_SELF_ENDING_WITH_VALUE)
        .at(1, 1);
  }


  public void testParseIfStmt() throws Exception {

    String templateBody =
        "  {if $zoo}{$zoo}{/if}\n" +
        "  {if $boo}\n" +
        "    Blah\n" +
        "  {elseif $foo.goo > 2}\n" +
        "    {$moo}\n" +
        "  {else}\n" +
        "    Blah {$moo}\n" +
        "  {/if}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(2, nodes.size());

    IfNode in0 = (IfNode) nodes.get(0);
    assertEquals(1, in0.numChildren());
    IfCondNode in0icn0 = (IfCondNode) in0.getChild(0);
    assertEquals("$zoo", in0icn0.getExprText());
    assertEquals(1, in0icn0.numChildren());
    assertEquals("$zoo", ((PrintNode) in0icn0.getChild(0)).getExprText());
    assertTrue(in0icn0.getExprUnion().getExpr().getRoot() instanceof VarRefNode);

    IfNode in1 = (IfNode) nodes.get(1);
    assertEquals(3, in1.numChildren());
    IfCondNode in1icn0 = (IfCondNode) in1.getChild(0);
    assertEquals("$boo", in1icn0.getExprText());
    assertTrue(in1icn0.getExprUnion().getExpr().getRoot() instanceof VarRefNode);
    IfCondNode in1icn1 = (IfCondNode) in1.getChild(1);
    assertEquals("$foo.goo > 2", in1icn1.getExprText());
    assertTrue(in1icn1.getExprUnion().getExpr().getRoot() instanceof GreaterThanOpNode);
    assertEquals("", ((IfElseNode) in1.getChild(2)).getCommandText());
    assertEquals("{if $boo}Blah{elseif $foo.goo > 2}{$moo}{else}Blah {$moo}{/if}",
                 in1.toSourceString());
  }


  public void testParseSwitchStmt() throws Exception {

    String templateBody =
        "  {switch $boo} {case 0}Blah\n" +
        "    {case $foo.goo}\n" +
        "      Bleh\n" +
        "    {case -1, 1, $moo}\n" +
        "      Bluh\n" +
        "    {default}\n" +
        "      Bloh\n" +
        "  {/switch}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    SwitchNode sn = (SwitchNode) nodes.get(0);
    assertEquals("$boo", sn.getExpr().toSourceString());
    assertTrue(sn.getExpr().getRoot() instanceof VarRefNode);
    assertEquals(4, sn.numChildren());

    SwitchCaseNode scn0 = (SwitchCaseNode) sn.getChild(0);
    assertEquals("0", scn0.getExprListText());
    assertEquals(1, scn0.getExprList().size());
    assertTrue(scn0.getExprList().get(0).getRoot() instanceof IntegerNode);

    SwitchCaseNode scn1 = (SwitchCaseNode) sn.getChild(1);
    assertEquals("$foo.goo", scn1.getExprListText());
    assertEquals(1, scn1.getExprList().size());
    assertTrue(scn1.getExprList().get(0).getRoot() instanceof FieldAccessNode);

    SwitchCaseNode scn2 = (SwitchCaseNode) sn.getChild(2);
    assertEquals("-1, 1, $moo", scn2.getExprListText());
    assertEquals(3, scn2.getExprList().size());
    assertTrue(scn2.getExprList().get(0).getRoot() instanceof NegativeOpNode);
    assertTrue(scn2.getExprList().get(1).getRoot() instanceof IntegerNode);
    assertTrue(scn2.getExprList().get(2).getRoot() instanceof VarRefNode);
    assertEquals("Bluh", ((RawTextNode) scn2.getChild(0)).getRawText());

    assertEquals(
        "Bloh", ((RawTextNode) ((SwitchDefaultNode) sn.getChild(3)).getChild(0)).getRawText());

    assertEquals(
        "{switch $boo}{case 0}Blah{case $foo.goo}Bleh{case -1, 1, $moo}Bluh{default}Bloh{/switch}",
        sn.toSourceString());
  }


  public void testParseForeachStmt() throws Exception {

    String templateBody =
        "  {foreach $goo in $goose}\n" +
        "    {$goose.numKids} goslings.{\\n}\n" +
        "  {/foreach}\n" +
        "  {foreach $boo in $foo.booze}\n" +
        "    Scary drink {$boo.name}!\n" +
        "    {if not isLast($boo)}{\\n}{/if}\n" +
        "  {ifempty}\n" +
        "    Sorry, no booze.\n" +
        "  {/foreach}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(2, nodes.size());

    ForeachNode fn0 = (ForeachNode) nodes.get(0);
    assertEquals("$goose", fn0.getExprText());
    assertTrue(fn0.getExpr().getRoot() instanceof VarRefNode);
    assertEquals(1, fn0.numChildren());

    ForeachNonemptyNode fn0fnn0 = (ForeachNonemptyNode) fn0.getChild(0);
    assertEquals("goo", fn0fnn0.getVarName());
    assertEquals(2, fn0fnn0.numChildren());
    assertEquals("$goose.numKids", ((PrintNode) fn0fnn0.getChild(0)).getExprText());
    assertEquals(" goslings.\n", ((RawTextNode) fn0fnn0.getChild(1)).getRawText());

    ForeachNode fn1 = (ForeachNode) nodes.get(1);
    assertEquals("$foo.booze", fn1.getExprText());
    assertTrue(fn1.getExpr().getRoot() instanceof FieldAccessNode);
    assertEquals(2, fn1.numChildren());

    ForeachNonemptyNode fn1fnn0 = (ForeachNonemptyNode) fn1.getChild(0);
    assertEquals("boo", fn1fnn0.getVarName());
    assertEquals("$foo.booze", fn1fnn0.getExprText());
    assertEquals("boo", fn1fnn0.getVarName());
    assertEquals(4, fn1fnn0.numChildren());
    IfNode fn1fnn0in = (IfNode) fn1fnn0.getChild(3);
    assertEquals(1, fn1fnn0in.numChildren());
    assertEquals("not isLast($boo)", ((IfCondNode) fn1fnn0in.getChild(0)).getExprText());

    ForeachIfemptyNode fn1fin1 = (ForeachIfemptyNode) fn1.getChild(1);
    assertEquals(1, fn1fin1.numChildren());
    assertEquals("Sorry, no booze.", ((RawTextNode) fn1fin1.getChild(0)).getRawText());
  }


  public void testParseForStmt() throws Exception {

    String templateBody =
        "  {for $i in range(1, $itemsLength + 1)}\n" +
        "    {msg desc=\"Numbered item.\"}\n" +
        "      {$i}: {$items[$i - 1]}{\\n}\n" +
        "    {/msg}\n" +
        "  {/for}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    ForNode fn = (ForNode) nodes.get(0);
    assertEquals("i", fn.getVarName());
    ForNode.RangeArgs rangeArgs = fn.getRangeArgs();
    assertThat(rangeArgs.increment()).isAbsent();
    assertEquals("1", rangeArgs.start().get().toSourceString());
    assertEquals("$itemsLength + 1", rangeArgs.limit().toSourceString());

    assertThat(rangeArgs.start().get().getRoot()).isInstanceOf(IntegerNode.class);
    assertThat(rangeArgs.limit().getRoot()).isInstanceOf(PlusOpNode.class);

    assertEquals(1, fn.numChildren());
    MsgNode mn = ((MsgFallbackGroupNode) ((ForNode) nodes.get(0)).getChild(0)).getChild(0);
    assertEquals(4, mn.numChildren());
    assertEquals("$i",
        ((PrintNode) ((MsgPlaceholderNode) mn.getChild(0)).getChild(0)).getExprText());
    assertEquals("$items[$i - 1]",
        ((PrintNode) ((MsgPlaceholderNode) mn.getChild(2)).getChild(0)).getExprText());
  }


  @SuppressWarnings({"ConstantConditions"})
  public void testParseBasicCallStmt() throws Exception {

    String templateBody =
        "  {call .booTemplate_ /}\n" +
        "  {call foo.goo.mooTemplate data=\"all\" /}\n" +
        "  {call .booTemplate_ /}\n" +
        "  {call .zooTemplate data=\"$animals\"}\n" +
        "    {param yoo: round($too) /}\n" +
        "    {param woo}poo{/param}\n" +
        "    {param zoo: 0 /}\n" +
        "    {param doo kind=\"html\"}doopoo{/param}\n" +
        "  {/call}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertThat(nodes).hasSize(4);

    CallBasicNode cn0 = (CallBasicNode) nodes.get(0);
    assertTrue(cn0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(null, cn0.getCalleeName());
    assertEquals(".booTemplate_", cn0.getSrcCalleeName());
    assertEquals(false, cn0.dataAttribute().isPassingData());
    assertEquals(false, cn0.dataAttribute().isPassingAllData());
    assertEquals(null, cn0.dataAttribute().dataExpr());
    assertEquals("XXX", cn0.genBasePhName());
    assertEquals(0, cn0.numChildren());

    CallBasicNode cn1 = (CallBasicNode) nodes.get(1);
    assertTrue(cn1.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(null, cn1.getCalleeName());
    assertEquals("foo.goo.mooTemplate", cn1.getSrcCalleeName());
    assertEquals(true, cn1.dataAttribute().isPassingData());
    assertEquals(true, cn1.dataAttribute().isPassingAllData());
    assertEquals(null, cn1.dataAttribute().dataExpr());
    assertFalse(cn1.genSamenessKey().equals(cn0.genSamenessKey()));
    assertEquals(0, cn1.numChildren());

    CallBasicNode cn2 = (CallBasicNode) nodes.get(2);
    assertTrue(cn2.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(null, cn2.getCalleeName());
    assertEquals(".booTemplate_", cn2.getSrcCalleeName());
    assertFalse(cn2.dataAttribute().isPassingData());
    assertEquals(false, cn2.dataAttribute().isPassingAllData());
    assertEquals(null, cn2.dataAttribute().dataExpr());
    assertEquals("XXX", cn2.genBasePhName());
    assertEquals(0, cn2.numChildren());

    CallBasicNode cn3 = (CallBasicNode) nodes.get(3);
    assertTrue(cn3.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(null, cn3.getCalleeName());
    assertEquals(".zooTemplate", cn3.getSrcCalleeName());
    assertEquals(true, cn3.dataAttribute().isPassingData());
    assertEquals(false, cn3.dataAttribute().isPassingAllData());
    assertTrue(cn3.dataAttribute().dataExpr().getRoot() != null);
    assertEquals("$animals", cn3.dataAttribute().dataExpr().toSourceString());
    assertEquals(4, cn3.numChildren());

    {
      final CallParamValueNode cn4cpvn0 = (CallParamValueNode) cn3.getChild(0);
      assertEquals("yoo", cn4cpvn0.getKey());
      assertEquals("round($too)", cn4cpvn0.getValueExprText());
      assertTrue(cn4cpvn0.getValueExprUnion().getExpr().getRoot() instanceof FunctionNode);
    }

    {
      final CallParamContentNode cn4cpcn1 = (CallParamContentNode) cn3.getChild(1);
      assertEquals("woo", cn4cpcn1.getKey());
      assertNull(cn4cpcn1.getContentKind());
      assertEquals("poo", ((RawTextNode) cn4cpcn1.getChild(0)).getRawText());
    }

    {
      final CallParamValueNode cn4cpvn2 = (CallParamValueNode) cn3.getChild(2);
      assertEquals("zoo", cn4cpvn2.getKey());
      assertEquals("0", cn4cpvn2.getValueExprText());
    }

    {
      final CallParamContentNode cn4cpcn3 = (CallParamContentNode) cn3.getChild(3);
      assertEquals("doo", cn4cpcn3.getKey());
      assertNotNull(cn4cpcn3.getContentKind());
      assertEquals(ContentKind.HTML, cn4cpcn3.getContentKind());
      assertEquals("doopoo", ((RawTextNode) cn4cpcn3.getChild(0)).getRawText());
    }
  }


  @SuppressWarnings({"ConstantConditions"})
  public void testParseDelegateCallStmt() throws Exception {

    String templateBody =
        "  {delcall booTemplate /}\n" +
        "  {delcall foo.goo.mooTemplate data=\"all\" /}\n" +
        "  {delcall MySecretFeature.zooTemplate data=\"$animals\"}\n" +
        "    {param yoo: round($too) /}\n" +
        "    {param woo}poo{/param}\n" +
        "  {/delcall}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(3, nodes.size());

    CallDelegateNode cn0 = (CallDelegateNode) nodes.get(0);
    assertTrue(cn0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("booTemplate", cn0.getDelCalleeName());
    assertEquals(false, cn0.dataAttribute().isPassingData());
    assertEquals(false, cn0.dataAttribute().isPassingAllData());
    assertEquals(null, cn0.dataAttribute().dataExpr());
    assertEquals("XXX", cn0.genBasePhName());
    assertEquals(0, cn0.numChildren());

    CallDelegateNode cn1 = (CallDelegateNode) nodes.get(1);
    assertTrue(cn1.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("foo.goo.mooTemplate", cn1.getDelCalleeName());
    assertEquals(true, cn1.dataAttribute().isPassingData());
    assertEquals(true, cn1.dataAttribute().isPassingAllData());
    assertEquals(null, cn1.dataAttribute().dataExpr());
    assertFalse(cn1.genSamenessKey().equals(cn0.genSamenessKey()));
    assertEquals(0, cn1.numChildren());

    CallDelegateNode cn2 = (CallDelegateNode) nodes.get(2);
    assertTrue(cn2.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("MySecretFeature.zooTemplate", cn2.getDelCalleeName());
    assertEquals(true, cn2.dataAttribute().isPassingData());
    assertEquals(false, cn2.dataAttribute().isPassingAllData());
    assertTrue(cn2.dataAttribute().dataExpr().getRoot() != null);
    assertEquals("$animals", cn2.dataAttribute().dataExpr().toSourceString());
    assertEquals(2, cn2.numChildren());

    CallParamValueNode cn2cpvn0 = (CallParamValueNode) cn2.getChild(0);
    assertEquals("yoo", cn2cpvn0.getKey());
    assertEquals("round($too)", cn2cpvn0.getValueExprText());
    assertTrue(cn2cpvn0.getValueExprUnion().getExpr().getRoot() instanceof FunctionNode);

    CallParamContentNode cn2cpcn1 = (CallParamContentNode) cn2.getChild(1);
    assertEquals("woo", cn2cpcn1.getKey());
    assertEquals("poo", ((RawTextNode) cn2cpcn1.getChild(0)).getRawText());
  }


  @SuppressWarnings({"ConstantConditions"})
  public void testParseCallStmtWithPhname() throws Exception {

    String templateBody = "" +
        "  {call .booTemplate_ phname=\"booTemplate_\" /}\n" +
        "  {call .booTemplate_ phname=\"booTemplate_\" /}\n" +
        "  {delcall MySecretFeature.zooTemplate data=\"$animals\" phname=\"secret_zoo\"}\n" +
        "    {param zoo: 0 /}\n" +
        "  {/delcall}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(3, nodes.size());

    CallBasicNode cn0 = (CallBasicNode) nodes.get(0);
    assertEquals("BOO_TEMPLATE", cn0.genBasePhName());
    assertTrue(cn0.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals(null, cn0.getCalleeName());
    assertEquals(".booTemplate_", cn0.getSrcCalleeName());
    assertEquals(false, cn0.dataAttribute().isPassingData());
    assertEquals(false, cn0.dataAttribute().isPassingAllData());
    assertEquals(null, cn0.dataAttribute().dataExpr());
    assertEquals(0, cn0.numChildren());

    CallBasicNode cn1 = (CallBasicNode) nodes.get(1);

    CallDelegateNode cn2 = (CallDelegateNode) nodes.get(2);
    assertEquals("SECRET_ZOO", cn2.genBasePhName());
    assertTrue(cn2.couldHaveSyntaxVersionAtLeast(SyntaxVersion.V2_0));
    assertEquals("MySecretFeature.zooTemplate", cn2.getDelCalleeName());
    assertEquals(true, cn2.dataAttribute().isPassingData());
    assertEquals(false, cn2.dataAttribute().isPassingAllData());
    assertTrue(cn2.dataAttribute().dataExpr().getRoot() != null);
    assertEquals("$animals", cn2.dataAttribute().dataExpr().toSourceString());
    assertEquals(1, cn2.numChildren());

    assertFalse(cn0.genSamenessKey().equals(cn1.genSamenessKey()));  // CallNodes are never same
    assertFalse(cn2.genSamenessKey().equals(cn0.genSamenessKey()));
  }


  public void testParseLogStmt() throws Exception {

    String templateBody = "{log}Blah {$foo}.{/log}";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    LogNode logNode = (LogNode) nodes.get(0);
    assertEquals(3, logNode.numChildren());
    assertEquals("Blah ", ((RawTextNode) logNode.getChild(0)).getRawText());
    assertEquals("$foo", ((PrintNode) logNode.getChild(1)).getExprText());
  }


  public void testParseDebuggerStmt() throws Exception {

    String templateBody = "{debugger}";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    assertTrue(nodes.get(0) instanceof DebuggerNode);
  }


  // -----------------------------------------------------------------------------------------------
  // Tests for plural/select messages.


  public void testParseMsgStmtWithPlural() throws Exception {
    String templateBody =
      "  {msg desc=\"A sample plural message\"}\n" +
      "    {plural $num_people offset=\"1\"}\n" +
      "      {case 0}I see no one in {$place}.\n" +
      "      {case 1}I see {$person} in {$place}.\n" +
      "      {case 2}I see {$person} and one other person in {$place}.\n" +
      "      {default}I see {$person} and {remainder($num_people)} other people in {$place}.\n" +
      "    {/plural}" +
      "  {/msg}\n";


    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgNode mn = ((MsgFallbackGroupNode) nodes.get(0)).getChild(0);
    assertEquals(1, mn.numChildren());
    assertEquals("A sample plural message", mn.getDesc());

    MsgPluralNode pn = (MsgPluralNode) mn.getChild(0);
    assertEquals("$num_people offset=\"1\"", pn.getCommandText());
    assertEquals(1, pn.getOffset());
    assertEquals(4, pn.numChildren());   // 3 cases and default

    // Case 0
    MsgPluralCaseNode cn0 = (MsgPluralCaseNode) pn.getChild(0);
    assertEquals(3, cn0.numChildren());
    assertEquals(0, cn0.getCaseNumber());

    RawTextNode rtn01 = (RawTextNode) cn0.getChild(0);
    assertEquals("I see no one in ", rtn01.getRawText());

    MsgPlaceholderNode phn01 = (MsgPlaceholderNode) cn0.getChild(1);
    assertEquals("{$place}", phn01.toSourceString());

    RawTextNode rtn02 = (RawTextNode) cn0.getChild(2);
    assertEquals(".", rtn02.getRawText());

    // Case 1
    MsgPluralCaseNode cn1 = (MsgPluralCaseNode) pn.getChild(1);
    assertEquals(5, cn1.numChildren());
    assertEquals(1, cn1.getCaseNumber());

    RawTextNode rtn11 = (RawTextNode) cn1.getChild(0);
    assertEquals("I see ", rtn11.getRawText());

    MsgPlaceholderNode phn11 = (MsgPlaceholderNode) cn1.getChild(1);
    assertEquals("{$person}", phn11.toSourceString());

    RawTextNode rtn12 = (RawTextNode) cn1.getChild(2);
    assertEquals(" in ", rtn12.getRawText());

    MsgPlaceholderNode phn12 = (MsgPlaceholderNode) cn1.getChild(3);
    assertEquals("{$place}", phn12.toSourceString());

    RawTextNode rtn13 = (RawTextNode) cn1.getChild(4);
    assertEquals(".", rtn13.getRawText());

    // Case 2
    MsgPluralCaseNode cn2 = (MsgPluralCaseNode) pn.getChild(2);
    assertEquals(5, cn2.numChildren());
    assertEquals(2, cn2.getCaseNumber());

    RawTextNode rtn21 = (RawTextNode) cn2.getChild(0);
    assertEquals("I see ", rtn21.getRawText());

    MsgPlaceholderNode phn21 = (MsgPlaceholderNode) cn2.getChild(1);
    assertEquals("{$person}", phn21.toSourceString());

    RawTextNode rtn22 = (RawTextNode) cn2.getChild(2);
    assertEquals(" and one other person in ", rtn22.getRawText());

    MsgPlaceholderNode phn22 = (MsgPlaceholderNode) cn2.getChild(3);
    assertEquals("{$place}", phn22.toSourceString());

    RawTextNode rtn23 = (RawTextNode) cn2.getChild(4);
    assertEquals(".", rtn23.getRawText());

    // Default
    MsgPluralDefaultNode dn = (MsgPluralDefaultNode) pn.getChild(3);
    assertEquals(7, dn.numChildren());

    RawTextNode rtnd1 = (RawTextNode) dn.getChild(0);
    assertEquals("I see ", rtnd1.getRawText());

    MsgPlaceholderNode phnd1 = (MsgPlaceholderNode) dn.getChild(1);
    assertEquals("{$person}", phnd1.toSourceString());

    RawTextNode rtnd2 = (RawTextNode) dn.getChild(2);
    assertEquals(" and ", rtnd2.getRawText());

    MsgPlaceholderNode phnd2 = (MsgPlaceholderNode) dn.getChild(3);
    assertEquals("{remainder($num_people)}", phnd2.toSourceString());

    RawTextNode rtnd3 = (RawTextNode) dn.getChild(4);
    assertEquals(" other people in ", rtnd3.getRawText());

    MsgPlaceholderNode phnd3 = (MsgPlaceholderNode) dn.getChild(5);
    assertEquals("{$place}", phnd3.toSourceString());

    RawTextNode rtnd4 = (RawTextNode) dn.getChild(6);
    assertEquals(".", rtnd4.getRawText());
  }


  public void testParseMsgStmtWithSelect() throws Exception {
    String templateBody =
        "{msg desc=\"A sample gender message\"}\n" +
        "  {select $gender}\n" +
        "    {case 'female'}{$person} added you to her circle.\n" +
        "    {default}{$person} added you to his circle.\n" +
        "  {/select}\n" +
        "{/msg}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgNode mn = ((MsgFallbackGroupNode) nodes.get(0)).getChild(0);
    assertEquals(1, mn.numChildren());
    assertEquals("A sample gender message", mn.getDesc());

    MsgSelectNode sn = (MsgSelectNode) mn.getChild(0);
    assertEquals("$gender", sn.getCommandText());
    assertEquals(2, sn.numChildren());   // female and default

    // Case 'female'
    MsgSelectCaseNode cnf = (MsgSelectCaseNode) sn.getChild(0);
    assertEquals("'female'", cnf.getCommandText());
    assertEquals(2, cnf.numChildren());

    MsgPlaceholderNode phnf = (MsgPlaceholderNode) cnf.getChild(0);
    assertEquals("{$person}", phnf.toSourceString());

    RawTextNode rtnf = (RawTextNode) cnf.getChild(1);
    assertEquals(" added you to her circle.", rtnf.getRawText());

    // Default
    MsgSelectDefaultNode dn = (MsgSelectDefaultNode) sn.getChild(1);
    assertEquals(2, dn.numChildren());

    MsgPlaceholderNode phnd = (MsgPlaceholderNode) dn.getChild(0);
    assertEquals("{$person}", phnd.toSourceString());

    RawTextNode rtnd = (RawTextNode) dn.getChild(1);
    assertEquals(" added you to his circle.", rtnd.getRawText());
  }


  public void testParseMsgStmtWithNestedSelects() throws Exception {
    String templateBody =
        "{msg desc=\"A sample nested message\"}\n" +
        "  {select $gender1}\n" +
        "    {case 'female'}\n" +
        "      {select $gender2}\n" +
        "        {case 'female'}{$person1} added {$person2} and her friends to her circle.\n" +
        "        {default}{$person1} added {$person2} and his friends to her circle.\n" +
        "      {/select}\n" +
        "    {default}\n" +
        "      {select $gender2}\n" +
        "        {case 'female'}{$person1} put {$person2} and her friends to his circle.\n" +
        "        {default}{$person1} put {$person2} and his friends to his circle.\n" +
        "      {/select}\n" +
        "  {/select}\n" +
        "{/msg}\n";

    List<StandaloneNode> nodes = parseTemplateBody(templateBody, FAIL).getBodyNodes();
    assertEquals(1, nodes.size());

    MsgNode mn = ((MsgFallbackGroupNode) nodes.get(0)).getChild(0);
    assertEquals(1, mn.numChildren());
    assertEquals("A sample nested message", mn.getDesc());

    // Outer select
    MsgSelectNode sn = (MsgSelectNode) mn.getChild(0);
    assertEquals("$gender1", sn.getCommandText());
    assertEquals(2, sn.numChildren());   // female and default

    // Outer select: Case 'female'
    MsgSelectCaseNode cnf = (MsgSelectCaseNode) sn.getChild(0);
    assertEquals("'female'", cnf.getCommandText());
    assertEquals(1, cnf.numChildren()); // Another select

    // Outer select: Case 'female': Inner select
    MsgSelectNode sn2 = (MsgSelectNode) cnf.getChild(0);
    assertEquals("$gender2", sn2.getCommandText());
    assertEquals(2, sn2.numChildren());   // female and default

    // Outer select: Case 'female': Inner select: Case 'female'
    MsgSelectCaseNode cnf2 = (MsgSelectCaseNode) sn2.getChild(0);
    assertEquals("'female'", cnf2.getCommandText());
    assertEquals(4, cnf2.numChildren());

    // Outer select: Case 'female': Inner select: Case 'female': Placeholder $person1
    MsgPlaceholderNode phn1 = (MsgPlaceholderNode) cnf2.getChild(0);
    assertEquals("{$person1}", phn1.toSourceString());

    // Outer select: Case 'female': Inner select: Case 'female': RawText
    RawTextNode rtn1 = (RawTextNode) cnf2.getChild(1);
    assertEquals(" added ", rtn1.getRawText());

    // Outer select: Case 'female': Inner select: Case 'female': Placeholder $person2
    MsgPlaceholderNode phn2 = (MsgPlaceholderNode) cnf2.getChild(2);
    assertEquals("{$person2}", phn2.toSourceString());

    // Outer select: Case 'female': Inner select: Case 'female': RawText
    RawTextNode rtn2 = (RawTextNode) cnf2.getChild(3);
    assertEquals(" and her friends to her circle.", rtn2.getRawText());

    // Outer select: Case 'female': Inner select: Default
    MsgSelectDefaultNode dn2 = (MsgSelectDefaultNode) sn2.getChild(1);
    assertEquals(4, dn2.numChildren());

    // Outer select: Case 'female': Inner select: Default: Placeholder $person1
    MsgPlaceholderNode phn21 = (MsgPlaceholderNode) dn2.getChild(0);
    assertEquals("{$person1}", phn21.toSourceString());

    // Outer select: Case 'female': Inner select: Default: RawText
    RawTextNode rtn21 = (RawTextNode) dn2.getChild(1);
    assertEquals(" added ", rtn21.getRawText());

    // Outer select: Case 'female': Inner select: Default: Placeholder $person2
    MsgPlaceholderNode phn22 = (MsgPlaceholderNode) dn2.getChild(2);
    assertEquals("{$person2}", phn22.toSourceString());

    // Outer select: Case 'female': Inner select: Default: RawText
    RawTextNode rtn22 = (RawTextNode) dn2.getChild(3);
    assertEquals(" and his friends to her circle.", rtn22.getRawText());

    // Outer select: Default
    MsgSelectDefaultNode dn = (MsgSelectDefaultNode) sn.getChild(1);
    assertEquals(1, dn.numChildren()); // Another select

    // Outer select: Default: Inner select
    MsgSelectNode sn3 = (MsgSelectNode) dn.getChild(0);
    assertEquals("$gender2", sn3.getCommandText());
    assertEquals(2, sn3.numChildren());   // female and default

    // Outer select: Default: Inner select: Case 'female'
    MsgSelectCaseNode cnf3 = (MsgSelectCaseNode) sn3.getChild(0);
    assertEquals("'female'", cnf3.getCommandText());
    assertEquals(4, cnf3.numChildren());

    // Outer select: Default: Inner select: Case 'female': Placeholder $person1
    MsgPlaceholderNode phn3 = (MsgPlaceholderNode) cnf3.getChild(0);
    assertEquals("{$person1}", phn3.toSourceString());

    // Outer select: Default: Inner select: Case 'female': RawText
    RawTextNode rtn3 = (RawTextNode) cnf3.getChild(1);
    assertEquals(" put ", rtn3.getRawText());

    // Outer select: Default: Inner select: Case 'female': Placeholder $person2
    MsgPlaceholderNode phn4 = (MsgPlaceholderNode) cnf3.getChild(2);
    assertEquals("{$person2}", phn4.toSourceString());

    // Outer select: Default: Inner select: Case 'female': RawText
    RawTextNode rtn4 = (RawTextNode) cnf3.getChild(3);
    assertEquals(" and her friends to his circle.", rtn4.getRawText());

    // Outer select: Default: Inner select: Default
    MsgSelectDefaultNode dn3 = (MsgSelectDefaultNode) sn3.getChild(1);
    assertEquals(4, dn3.numChildren());

    // Outer select: Default: Inner select: Default: Placeholder $person1
    MsgPlaceholderNode phn5 = (MsgPlaceholderNode) dn3.getChild(0);
    assertEquals("{$person1}", phn5.toSourceString());

    // Outer select: Default: Inner select: Default: RawText
    RawTextNode rtn5 = (RawTextNode) dn3.getChild(1);
    assertEquals(" put ", rtn5.getRawText());

    // Outer select: Default: Inner select: Default: Placeholder $person2
    MsgPlaceholderNode phn6 = (MsgPlaceholderNode) dn3.getChild(2);
    assertEquals("{$person2}", phn6.toSourceString());

    // Outer select: Default: Inner select: Default: RawText
    RawTextNode rtn6 = (RawTextNode) dn3.getChild(3);
    assertEquals(" and his friends to his circle.", rtn6.getRawText());
  }

  public void testMultipleErrors() throws ParseException {
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    parseTemplateBody(
        "{call 123 /}\n" // Invalid callee name "123" for 'call' command.
            + "{delcall 123 /}\n" // Invalid delegate name "123" for 'delcall' command.
            + "{foreach foo in bar}{/foreach}\n" // Invalid 'foreach' command text "foo in bar".
            + "{let /}\n", errorReporter); // Invalid 'let' command text "".
    List<String> errors = errorReporter.getErrorMessages();
    assertThat(errors).hasSize(5);
    assertThat(errors.get(0)).contains("Invalid callee name \"123\" for 'call' command.");
    assertThat(errors.get(1)).contains("Invalid delegate name \"123\" for 'delcall' command.");
    assertThat(errors.get(2)).contains("Invalid 'foreach' command text \"foo in bar\".");
    assertThat(errors.get(3)).contains("Invalid 'let' command text.");
    assertThat(errors.get(4)).contains(
        "A 'let' tag should be self-ending (with a trailing '/') if and only if it also contains "
            + "a value (invalid tag is {let  /}).");
  }


  // -----------------------------------------------------------------------------------------------
  // Helpers.


  /**
   * Parses the given input as a template body.
   * @param input The input string to parse.
   * @throws TokenMgrError When the given input has a token error.
   * @throws ParseException When the given input has a parse error.
   * @return The parse tree nodes created.
   */
  private static TemplateParseResult parseTemplateBody(String input, ErrorReporter errorReporter)
      throws TokenMgrError, ParseException {
    TemplateParseResult result = parseTemplateContent(input, errorReporter);
    assertNull(result.getHeaderDecls());
    return result;
  }


  /**
   * Parses the given input as a template content (header and body).
   * @param input The input string to parse.
   * @throws TokenMgrError When the given input has a token error.
   * @return The decl infos and parse tree nodes created.
   */
  private static TemplateParseResult parseTemplateContent(String input, ErrorReporter errorReporter)
      throws ParseException {
    return new TemplateParser(
            new IncrementingIdGenerator(),
            input,
            "test.soy",
            1, // start line number
            0, // start col number
            errorReporter)
        .parseTemplateContent();
  }


  /**
   * Parses the given input using the MaybeWhitespace rule.
   * @param errorMessage The error message MaybeWhitespace should emit when it fails.
   * @param input The input string to parse.
   * @throws TokenMgrError When the given input has a token error.
   * @throws ParseException When the given input has a parse error.
   */
  private static void parseMaybeWhitespace(String errorMessage, String input)
      throws TokenMgrError, ParseException {
    new TemplateParser(
            new IncrementingIdGenerator(),
            input,
            "test.soy",
            1, // start line number
            0, // start col number
            ExplodingErrorReporter.get())
        .MaybeWhitespace(errorMessage);
  }


  /**
   * Asserts that the given input is a valid template.
   * @param input The input string to parse.
   * @throws TokenMgrError When the given input has a token error.
   * @throws ParseException When the given input has a parse error.
   */
  private static void assertIsTemplateBody(String input) throws TokenMgrError, ParseException {
    TemplateSubject.assertThatTemplateContent(input).isWellFormed();
  }


  /**
   * Asserts that the given input is a valid template content (header and body).
   * @param input The input string to parse.
   * @throws TokenMgrError When the given input has a token error.
   * @throws ParseException When the given input has a parse error.
   */
  private static void assertIsTemplateContent(String input) throws TokenMgrError, ParseException {
    TemplateSubject.assertThatTemplateContent(input).isWellFormed();
  }


  /**
   * Asserts that the given input is not a valid template.
   * @param input The input string to parse.
   * @throws AssertionFailedError When the given input is actually a valid template.
   */
  private static void assertIsNotTemplateBody(String input) throws AssertionFailedError {
    TemplateSubject.assertThatTemplateContent(input).isNotWellFormed();
  }


  /**
   * Asserts that the given input is not a valid template content (header and body).
   * @param input The input string to parse.
   * @throws AssertionFailedError When the given input is actually a valid template.
   */
  private static void assertIsNotTemplateContent(String input) throws AssertionFailedError {
    TemplateSubject.assertThatTemplateContent(input).isNotWellFormed();
  }

}
