/*
 * Copyright 2013 Google Inc.
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

package com.google.template.soy.parsepasses.contextautoesc;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.parsepasses.contextautoesc.Context.State;
import com.google.template.soy.parsepasses.contextautoesc.SlicedRawTextNode.RawTextSlice;
import com.google.template.soy.soytree.ExprUnion;
import com.google.template.soy.soytree.IfCondNode;
import com.google.template.soy.soytree.IfNode;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.RawTextNode;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.defn.InjectedParam;
import com.google.template.soy.types.primitive.StringType;

import java.util.Collections;
import java.util.List;

/**
 * Inserts attributes into templates to bless inline {@code <script>} and {@code <style>} elements
 * and inline event handler and style attributes so that the browser can distinguish scripts
 * specified by the template author from ones injected via XSS.
 *
 * This class converts templates by adding {@code nonce="..."} to {@code <script>} and
 * {@code <style>} elements, so
 * <blockquote>
 * {@code <script>...</script>}
 * </blockquote>
 * becomes
 * <blockquote>
 * {@code <script{if $ij.csp_nonce} nonce="{$ij.csp_nonce}"{/if}>...</script>}
 * </blockquote>
 * which authorize scripts in HTML pages that are governed by the <i>Content Security Policy</i>.
 *
 * <p>
 * This class assumes that the value of {@code $ij.csp_nonce} will either be null or a valid
 * <a href="//dvcs.w3.org/hg/content-security-policy/raw-file/tip/csp-specification.dev.html#dfn-a-valid-nonce"
 * >CSP-style "nonce"</a>, an unguessable string consisting of Latin Alpha-numeric characters,
 * plus ({@code '+'}), and solidus ({@code '/'}).
 * <blockquote>
 *   {@code nonce-value = 1*( ALPHA / DIGIT / "+" / "/" )}
 * </blockquote>
 *
 * <h3>Dependencies</h3>
 * <p>
 * If inline event handlers or styles are used, then the page should also load
 * {@code security.CspVerifier} which verifies event handler values.
 *
 * <h3>Caveats</h3>
 * <p>
 * This class does not add any {@code <meta http-equiv="content-security-policy" ...>} elements to
 * the template.  The application developer must specify the CSP policy headers and include the
 * nonce there.
 *
 * <p>
 * Nonces should be of sufficient length, and from a crypto-strong source of randomness.
 * The stock <code>java.util.Random</code> is not strong enough, though a properly seeded
 * <code>SecureRandom</code> is ok.
 *
 */
public final class ContentSecurityPolicyPass {

  private ContentSecurityPolicyPass() {
    // Not instantiable.
  }


  /** The unprefixed name of the injected variable that holds the CSP nonce value for the page. */
  public static final String CSP_NONCE_VARIABLE_NAME = "csp_nonce";

  /** The name of the CSP nonce attribute, equals sign, and opening double quote. */
  private static final String NONCE_ATTR_BEFORE_VALUE = " nonce=\"";

  /** The closing double quote that appears after an attribute value. */
  private static final String ATTR_AFTER_VALUE = "\"";

  /**
   * A variable definition for {@code $ij.csp_nonce}.
   * Since this pass implicitly blesses scripts that appear in the template text, authors should not
   * explicitly mention {@code $id.csp_nonce} in their template signatures, so we do not look for a
   * declared variable definition.
   */
  private static final InjectedParam IMPLICIT_CSP_NONCE_DEFN =
      new InjectedParam(CSP_NONCE_VARIABLE_NAME, StringType.getInstance());


  // ---------------------------------------------------------------------------------------------
  // Predicates used to identify HTML element and attribute boundaries in templates.
  // ---------------------------------------------------------------------------------------------

  /**
   * True for any context that occurs within a {@code <script>} or {@code <style>} open tag.
   * {@code [START]} and {@code [END]} mark ranges of positions for which this predicate is true.
   * {@code <script[START] src=[END]foo[START]>[END]body()</script>}.
   */
  private static final Predicate<? super Context> IN_SCRIPT_OR_STYLE_TAG_PREDICATE =
      new Predicate<Context>() {
        public boolean apply(Context c) {
          return (
              // In a script tag or style,
              (c.elType == Context.ElementType.SCRIPT || c.elType == Context.ElementType.STYLE)
              && c.state == Context.State.HTML_TAG
              // but not in an attribute
              && c.attrType == Context.AttributeType.NONE
              );
        }
      };


  /**
   * True between the end of a {@code <script>} or {@code <style>}tag and the start of its end tag.
   * {@code [START]} and {@code [END]} mark ranges of positions for which this predicate is true.
   * {@code <script src=foo]>[START]body()[END]</script>}.
   */
  private static final Predicate<? super Context> IN_SCRIPT_OR_STYLE_BODY_PREDICATE =
      new Predicate<Context>() {
        public boolean apply(Context c) {
          return (
              // If we're not in an attribute,
              c.attrType == Context.AttributeType.NONE
              // but we're in JS or CSS, then we must be in a script or style body.
              && (c.state == Context.State.JS || c.state == Context.State.CSS)
              );
        }
      };


  /**
   * True immediately before an HTML attribute value.
   */
  public static final Predicate<? super Context> HTML_BEFORE_ATTRIBUTE_VALUE =
      new Predicate<Context>() {
        @Override
        public boolean apply(Context c) {
          return c.state == State.HTML_BEFORE_ATTRIBUTE_VALUE;
        }
      };


  /**
   * True inside an inline event handler value or style attribute.
   * {@code [START]} and {@code [END]} mark ranges of positions for which this predicate is true.
   * {@code <a onclick="[START]foo()"[END]>}.
   * Any close quote is part of the attribute value though the open quote is excluded as it is in
   * the BEFORE_ATTRIBUTE_VALUE state.
   */
  private static final Predicate<? super Context> IN_SCRIPT_OR_STYLE_ATTR_VALUE =
      new Predicate<Context>() {
        public boolean apply(Context c) {
          return c.elType != Context.ElementType.NONE
              && (isScriptAttr(c) || isStyleAttr(c));
        }

        private boolean isScriptAttr(Context c) {
          return c.attrType == Context.AttributeType.SCRIPT
              && c.state == Context.State.JS;
        }

        private boolean isStyleAttr(Context c) {
          return c.attrType == Context.AttributeType.STYLE
              && c.state == Context.State.CSS;
        }
      };


  // ---------------------------------------------------------------------------------------------
  // Generators for Soy nodes that mark JS as safe to run.
  // ---------------------------------------------------------------------------------------------

  /**
   * Generates Soy nodes to inject at a specific location in a raw text node.
   */
  private abstract static class InjectedSoyGenerator implements Comparable<InjectedSoyGenerator> {

    /** The raw text node into which to inject nodes. */
    final RawTextNode rawTextNode;

    /** The offset into rawTextNode's text at which to inject the nodes. */
    final int offset;

    /**
     * @param rawTextNode The raw text node into which to inject nodes.
     * @param offset the offset into rawTextNode's text at which to inject the nodes.
     */
    InjectedSoyGenerator(RawTextNode rawTextNode, int offset) {
      Preconditions.checkElementIndex(offset, rawTextNode.getRawText().length(), "text offset");
      this.rawTextNode = rawTextNode;
      this.offset = offset;
    }

    /**
     * Generates standalone Soy nodes to inject at {@link #offset} in {@link #rawTextNode} and adds
     * them to out.
     *
     * @param idGenerator generates IDs for newly created nodes.
     * @param out receives nodes to add in the order they should be added.
     */
    abstract void addNodesToInject(
        IdGenerator idGenerator, ImmutableList.Builder<? super SoyNode.StandaloneNode> out);

    /** Order first by raw text node ID and then by offset within the text node. */
    public final int compareTo(InjectedSoyGenerator other) {
      int delta = this.rawTextNode.getId() - other.rawTextNode.getId();
      if (delta == 0) {
        delta = this.offset - other.offset;
      }
      return delta;
    }
  }


  private static final class NonceAttrGenerator extends InjectedSoyGenerator {

    NonceAttrGenerator(RawTextNode rawTextNode, int offset) {
      super(rawTextNode, offset);
    }

    /** Adds `<code> nonce="{$ij.csp_nonce}"</code>`. */
    @Override
    void addNodesToInject(
        IdGenerator idGenerator, ImmutableList.Builder<? super SoyNode.StandaloneNode> out) {
      out.add(
          new RawTextNode(
              idGenerator.genId(), NONCE_ATTR_BEFORE_VALUE, rawTextNode.getSourceLocation()));
      out.add(makeInjectedCspNoncePrintNode(idGenerator));
      out.add(
          new RawTextNode(idGenerator.genId(), ATTR_AFTER_VALUE, rawTextNode.getSourceLocation()));
    }

  }


  private static final class InlineContentPrefixGenerator extends InjectedSoyGenerator {
    InlineContentPrefixGenerator(RawTextNode rawTextNode, int offset) {
      super(rawTextNode, offset);
    }

    /**
     * Adds `<code>/*{$ij.csp_verifier}*\/</code>` at the start of an event handler or style
     * attribute so that {@code template/security/csp_verify.js} can use a policy violation event
     * handler to lazily mark them safe to execute by prefix checking.
     * <p>
     * We use a block comment instead of several alternatives:
     * <ol>
     *   <li>A statement label prefix: {@code onclick="nonce:event_handler()"}</li>
     *   <li>A second attribute:
     *       {@code onclick="event_handler()" csp-safe="nonce event_handler()"}</li>
     *   <li>A cryptographic hash of the attribute value:
     *       {@code onclick="event_handler()" onclick-hash="A%^09t..."}</li>
     * </ol>
     * because each of these has at least one of these undesirable properties:
     * <ol>
     *   <li>Changes the meaning of {@code onclick="use strict; doStuff()"} because an event handler
     *       is a JS FunctionBody production, and only first statement of a FunctionBody may be a
     *       DirectivePrologue like a {@code "use strict"} directive, and that directive must be
     *       unlabelled.</li>
     *   <li>Requires translating nonces into JS identifiers or restricting nonces to a subset of
     *       the published grammar.</li>
     *   <li>Expands code size by duplicating large event handlers.</li>
     *   <li>Requires shipping large libraries like {@code goog.crypto}.</li>
     *   <li>Requires a separate mechanism for blessing inline styles.</li>
     * </ol>
     */
    @Override
    void addNodesToInject(
        IdGenerator idGenerator, ImmutableList.Builder<? super SoyNode.StandaloneNode> out) {
      // We re-use the CSP nonce as the inline-event-handler secret.
      out.add(new RawTextNode(idGenerator.genId(), "/*", rawTextNode.getSourceLocation()));
      out.add(makeInjectedCspNoncePrintNode(idGenerator));
      // Nonces may contain '/' but not '*' so the nonce will not be truncated as long as the nonce
      // generator produces valid nonces instead of arbitrary ASCII.
      out.add(new RawTextNode(idGenerator.genId(), "*/", rawTextNode.getSourceLocation()));
    }
  }


  /**
   * A group of InjectedSoyGenerators with the same raw text node and offset.
   */
  private static final class GroupOfInjectedSoyGenerator extends InjectedSoyGenerator {
    final ImmutableList<InjectedSoyGenerator> members;

    /**
     * @param group InjectedSoyGenerator with the same raw text node and offset.
     */
    GroupOfInjectedSoyGenerator(List<? extends InjectedSoyGenerator> group) {
      super(group.get(0).rawTextNode, group.get(0).offset);
      members = ImmutableList.copyOf(group);
      for (InjectedSoyGenerator member : members) {
        if (member.rawTextNode != rawTextNode || member.offset != offset) {
          throw new IllegalArgumentException("Invalid group member");
        }
      }
    }

    /** delegates to each member in-order to add nodes to out. */
    @Override
    void addNodesToInject(
        IdGenerator idGenerator, ImmutableList.Builder<? super SoyNode.StandaloneNode> out) {
      for (InjectedSoyGenerator member : members) {
        member.addNodesToInject(idGenerator, out);
      }
    }

  }


  // ---------------------------------------------------------------------------------------------
  // Soy tree traversal that injects Soy nodes to mark JS in templates as safe to run.
  // ---------------------------------------------------------------------------------------------

  /**
   * Add attributes to author-specified scripts and styles so that they will continue to run even
   * though the browser's CSP policy blocks injected scripts and styles.
   */
  public static void blessAuthorSpecifiedScripts(
      Iterable<? extends SlicedRawTextNode> slicedRawTextNodes) {
    // Given
    //   <script type="text/javascript">
    //     alert(1337)
    //   </script>
    // we want to produce
    //   <script type="text/javascript"{if $ij.csp_nonce} nonce="{$ij.csp_nonce}"{/if}>
    //     alert(1337)
    //   </script>
    // We need the nonce value to be unguessable which means not reliably reusing the same value
    // from one page render to the next.

    // We do this in several steps.
    // 1. Identify the start of the value of each inline event handler and style:
    //    <a onclick="foobar(this)">
    //                ^-- start
    // 2. Create an InlineContentPrefixGenerator instance that injects an prefix that can be used
    //    by javascript/security/csp_verifier.js to allow the event handler.
    // 3. Identify the end of each <script> and <style> tag.
    //    <script type="text/javascript">alert(1337)</script>
    //                                  ^-- Can insert more attributes here
    //    We use the contexts from the contextual auto-escaper to identify the boundary between
    //    the tag that starts a script element and its body.
    // 4. Walk backwards over ">" and "/>" to find a place where it is safe to insert atttributes.
    // 5. Create an InjectedSoyGenerator instance that encapsulates the content to insert.
    //    <script type="text/javascript">alert(1337)</script>
    //                                  ^-- Remember this location.
    // 6. Group InjectedSoyGenerators at the same location so that we could inject multiple chunks
    //    of content at the same slice offset.
    // 7. Create a conditional check at each unique location, {if $ij.csp_nonce}...{/if}, so that we
    //    don't insert CSP attributes when the template is applied without a secret.
    // 8. Create Soy nodes to fill out the {if}
    //    <script>  ->  <script{if $ij.csp_nonce} nonce="{$ij.csp_nonce}"{/if}>

    ImmutableList.Builder<InjectedSoyGenerator> injectedSoyGenerators = ImmutableList.builder();

    // We look for the end of attributes before the end of tags so that the stable sort we use to
    // group generators leaves any at attribute ends before the ones at the end of a tag.

    findCompleteInlineEventHandlers(slicedRawTextNodes, injectedSoyGenerators);

    findNonceAttrLocations(slicedRawTextNodes, injectedSoyGenerators);

    List<InjectedSoyGenerator> groupedInjectedAttrs = sortAndGroup(injectedSoyGenerators.build());

    generateAndInsertSoyNodesWrappedInIfNode(groupedInjectedAttrs);
  }


  /**
   * Handles steps 1 and 2 by finding event handler attributes that appear entirely within
   * a raw text node.
   */
  private static void findCompleteInlineEventHandlers(
      Iterable<? extends SlicedRawTextNode> slicedRawTextNodes,
      ImmutableList.Builder<InjectedSoyGenerator> out) {

    Iterable<RawTextSlice> valueSlices = SlicedRawTextNode.find(
        slicedRawTextNodes,
        HTML_BEFORE_ATTRIBUTE_VALUE,
        IN_SCRIPT_OR_STYLE_ATTR_VALUE,
        null /* nextContextPredicate */);

    // Step 1: identify the beginning of an inline event handler.
    for (SlicedRawTextNode.RawTextSlice valueSlice : valueSlices) {
      Context.AttributeEndDelimiter delimType = valueSlice.context.delimType;
      if (delimType != Context.AttributeEndDelimiter.DOUBLE_QUOTE
          && delimType != Context.AttributeEndDelimiter.SINGLE_QUOTE) {
        // Bail on unquoted event handlers since we might accidentally
        // bless an untrusted suffix as in
        //    <button onclick=foo(){if $c} bar={$c} {/if}{$d|noescape}>
        // where $d might merge into the content of onclick unnoticed if $c is almost always true.
        // If $d were ";doEvil()" then it would result in an injection.
        continue;
      }

      // Add a prefix after the open quote, which happens to be at the beginning of the slice.
      out.add(new InlineContentPrefixGenerator(
          valueSlice.slicedRawTextNode.getRawTextNode(), valueSlice.getStartOffset()));
    }
  }


  /**
   * Handles steps 3-5 by creating a NonceAttrGenerator for each location at the ^ in
   * {@code <script foo=bar^>} immediately after the run of attributes in a script tag.
   */
  private static void findNonceAttrLocations(
      Iterable<? extends SlicedRawTextNode> slicedRawTextNodes,
      ImmutableList.Builder<InjectedSoyGenerator> out) {

    // Step 3: identify slices that end a <script> element so we can find a location where it it is
    // safe to insert an attribute.
    for (SlicedRawTextNode.RawTextSlice slice : SlicedRawTextNode.find(
             slicedRawTextNodes, null,
             IN_SCRIPT_OR_STYLE_TAG_PREDICATE, IN_SCRIPT_OR_STYLE_BODY_PREDICATE)) {
      String rawText = slice.getRawText();
      int rawTextLen = rawText.length();
      // Step 4: find a safe place to insert attributes.
      if (rawText.charAt(rawTextLen - 1) != '>') {
        throw new IllegalStateException("Invalid tag end: " + rawText);
      }
      int insertionPoint = rawTextLen - 1;
      // We can't put an attribute in the middle of an XML-style "/>" tag terminator.
      if (insertionPoint - 1 >= 0 && rawText.charAt(insertionPoint - 1) == '/') {
        --insertionPoint;
      }

      // Step 5: create a generator for the CSP nonce attribute.
      out.add(new NonceAttrGenerator(
          slice.slicedRawTextNode.getRawTextNode(), slice.getStartOffset() + insertionPoint));
    }
  }


  /**
   * Handles step 6 by converting a list of InjectedSoyGenerators into an equivalent list where
   * there is only one per text node and offset, and where the list is sorted by text node ID and
   * offset.
   */
  private static List<InjectedSoyGenerator> sortAndGroup(List<InjectedSoyGenerator> ungrouped) {
    // Sort by node ID & offset
    ungrouped = Lists.newArrayList(ungrouped);
    Collections.sort(ungrouped);

    // Walk over list grouping members with the same raw text node and offset.
    ImmutableList.Builder<InjectedSoyGenerator> grouped = ImmutableList.builder();
    int n = ungrouped.size();
    for (int i = 0, end; i < n; i = end) {
      InjectedSoyGenerator firstGroupMember = ungrouped.get(i);
      end = i + 1;
      while (end < n && ungrouped.get(end).rawTextNode == firstGroupMember.rawTextNode
             && ungrouped.get(end).offset == firstGroupMember.offset) {
        ++end;
      }
      grouped.add(new GroupOfInjectedSoyGenerator(ungrouped.subList(i, end)));
    }
    return grouped.build();
  }


  /**
   * Handles steps 7 and 8 by applying the generators to create Soy nodes and injects them at the
   * location in the template specified by {@link InjectedSoyGenerator#rawTextNode} and
   * {@link InjectedSoyGenerator#offset}, splitting and replacing text nodes as necessary.
   *
   * <p>
   * {@link RawTextNode}'s text cannot be changed, so generators with the same {@link RawTextNode}
   * cannot be applied separately.  This method takes a list of generators, so it can apply them in
   * a batch and avoid conflicts.
   *
   * @param injectedSoyGenerators A sorted, grouped, list of generators.
   */
  private static void generateAndInsertSoyNodesWrappedInIfNode(
      List<? extends InjectedSoyGenerator> injectedSoyGenerators) {
    int n = injectedSoyGenerators.size();
    for (int i = 0, end; i < n; i = end) {
      // Group by RawTextNode.
      end = i + 1;
      InjectedSoyGenerator first = injectedSoyGenerators.get(i);
      while (end < n) {
        InjectedSoyGenerator atEnd = injectedSoyGenerators.get(end);
        if (first.rawTextNode == atEnd.rawTextNode) {
          ++end;
        } else {
          break;
        }
      }

      // Find the text node that we're going to split and inject into.
      RawTextNode rawTextNode = first.rawTextNode;
      String rawText = rawTextNode.getRawText();
      SoyNode.BlockNode parent = rawTextNode.getParent();
      IdGenerator idGenerator =
          parent.getNearestAncestor(SoyFileSetNode.class).getNodeIdGenerator();

      // Split rawTextNode on the offsets, and at each split, insert a nonce value.
      int textStart = 0;
      int childIndex = parent.getChildIndex(rawTextNode);
      parent.removeChild(rawTextNode);
      for (InjectedSoyGenerator generator : injectedSoyGenerators.subList(i, end)) {
        int offset = generator.offset;
        if (offset != textStart) {
          RawTextNode textBefore = new RawTextNode(
              idGenerator.genId(),
              rawText.substring(textStart, offset),
              rawTextNode.getSourceLocation());
          parent.addChild(childIndex, textBefore);
          ++childIndex;
          textStart = offset;
        }

        // Step 7: add an {if $ij.csp_nonce}...{/if} to prevent generation of CSP nonce when the
        // template is applied without a secret.
        IfNode ifNode = new IfNode(idGenerator.genId(), rawTextNode.getSourceLocation());
        IfCondNode ifCondNode = new IfCondNode(
            idGenerator.genId(),
            rawTextNode.getSourceLocation(),
            "if",
            new ExprUnion(makeReferenceToInjectedCspNonce()));
        parent.addChild(childIndex, ifNode);
        ++childIndex;
        ifNode.addChild(ifCondNode);

        // Step 8: inject Soy nodes into the {if}.
        ImmutableList.Builder<SoyNode.StandaloneNode> newChildren = ImmutableList.builder();
        generator.addNodesToInject(idGenerator, newChildren);
        ifCondNode.addChildren(newChildren.build());
      }

      if (textStart != rawText.length()) {
        RawTextNode textTail = new RawTextNode(
            idGenerator.genId(),
            rawText.substring(textStart),
            rawTextNode.getSourceLocation());
        parent.addChild(childIndex, textTail);
      }
    }
  }


  // ---------------------------------------------------------------------------------------------
  // Methods to programmatically create Soy commands and expressions.
  // ---------------------------------------------------------------------------------------------

  /**
   * Builds the Soy expression {@code $ij.csp_nonce} with an appropriate type.
   */
  private static VarRefNode makeReferenceToInjectedCspNonce() {
    return new VarRefNode(
        CSP_NONCE_VARIABLE_NAME,
        SourceLocation.UNKNOWN,
        true /*injected*/,
        true /* nullSafeInjected */,
        IMPLICIT_CSP_NONCE_DEFN);
  }


  /**
   * Builds the Soy command {@code {$ij.csp_nonce}}.
   */
  private static PrintNode makeInjectedCspNoncePrintNode(IdGenerator idGenerator) {
    return new PrintNode.Builder(
        idGenerator.genId(),
        true,  // Implicit.  {$ij.csp_nonce} not {print $ij.csp_nonce}
        SourceLocation.UNKNOWN)
        .exprUnion(new ExprUnion(makeReferenceToInjectedCspNonce()))
        .build(ExplodingErrorReporter.get());
  }
}
