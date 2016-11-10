/*
 * Copyright 2009 Google Inc.
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

package com.google.common.css.compiler.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.css.SourceCode;

import java.util.List;

/**
 * A wrapper around the JavaCC generated GSS parser.
 *
 */
public class GssParser extends AbstractGssParser {

  private final List<SourceCode> sources;
  private ImmutableList<GssParserException> handledErrors = ImmutableList.of();

  public GssParser(List<SourceCode> sources) {
    this.sources = sources;
  }

  public GssParser(SourceCode source) {
    this(ImmutableList.of(source));
  }

  public CssTree parse() throws GssParserException {
    return parse(false);
  }

  public CssTree parse(boolean errorHandling) throws GssParserException {
    ParseResult result = parseInternal(sources, errorHandling);
    this.handledErrors = result.getHandledErrors();
    return result.getCssTree();
  }

  /**
   * Returns errors from previous call to parse().
   */
  public List<GssParserException> getHandledErrors() {
    return handledErrors;
  }

  @Override
  protected GssParserCC getParser() {
    return new GssParserCC(EMPTY_CHAR_STREAM);
  }
}
