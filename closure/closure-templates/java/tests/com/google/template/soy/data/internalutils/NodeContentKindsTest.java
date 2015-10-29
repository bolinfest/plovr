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

package com.google.template.soy.data.internalutils;

import com.google.common.collect.ImmutableSet;
import com.google.template.soy.data.SanitizedContent.ContentKind;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Unit tests for NodeContentKinds.
 *
 */
public class NodeContentKindsTest extends TestCase {

  public void testForAttributeValue() {
    assertEquals(ContentKind.HTML, NodeContentKinds.forAttributeValue("html"));
    assertEquals(ContentKind.TEXT, NodeContentKinds.forAttributeValue("text"));
    assertNull(NodeContentKinds.forAttributeValue("blarg"));
  }

  public void testGetAttributeValues() {
    Set<String> attributeValues = NodeContentKinds.getAttributeValues();
    assertEquals(
        ImmutableSet.of("attributes", "css", "html", "js", "text", "uri", "trusted_resource_uri"),
        attributeValues);
  }

  public void testToJsSanitizedContentCtorName() {
    assertEquals("soydata.SanitizedHtml",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.HTML));
    assertEquals("soydata.SanitizedHtmlAttribute",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.ATTRIBUTES));
    assertEquals("soydata.SanitizedCss",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.CSS));
    assertEquals("soydata.SanitizedUri",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.URI));
    assertEquals("soydata.SanitizedTrustedResourceUri",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.TRUSTED_RESOURCE_URI));
    assertEquals("soydata.SanitizedJs",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.JS));
    assertEquals("soydata.UnsanitizedText",
         NodeContentKinds.toJsSanitizedContentCtorName(ContentKind.TEXT));
  }

  public void testToJsSanitizedContentOrdainer() {
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedHtml",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.HTML));
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedHtmlAttribute",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.ATTRIBUTES));
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedCss",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.CSS));
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedUri",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.URI));
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedTrustedResourceUri",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.TRUSTED_RESOURCE_URI));
    assertEquals("soydata.VERY_UNSAFE.ordainSanitizedJs",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.JS));
    assertEquals("soydata.markUnsanitizedText",
         NodeContentKinds.toJsSanitizedContentOrdainer(ContentKind.TEXT));
  }

  public void testToJsSanitizedContentOrdainerForInternalBlocks() {
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedHtmlForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.HTML));
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedAttributesForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.ATTRIBUTES));
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedCssForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.CSS));
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedUriForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.URI));
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedTrustedResourceUriForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(
             ContentKind.TRUSTED_RESOURCE_URI));
    assertEquals("soydata.VERY_UNSAFE.$$ordainSanitizedJsForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.JS));
    assertEquals("soydata.$$markUnsanitizedTextForInternalBlocks",
         NodeContentKinds.toJsSanitizedContentOrdainerForInternalBlocks(ContentKind.TEXT));
  }
}
