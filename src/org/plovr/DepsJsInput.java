package org.plovr;

/**
 * {@link DepsJsInput} represents a generated deps.js file.
 */
class DepsJsInput extends AbstractJsInput {

  private final String js;

  /**
   * @param baseJs The base.js file that this deps.js file should be parallel to
   *     so that when base.js is included via a &lt;script> tag, it will include
   *     the correct deps.js without using CLOSURE_NO_DEPS.
   * @param js
   */
  public DepsJsInput(JsInput baseJs, String js) {
    super(createParallelDepsJsNameForBaseJs(baseJs));
    this.js = js;
  }

  @Override
  public String getCode() {
    return js;
  }

  private static String createParallelDepsJsNameForBaseJs(JsInput baseJs) {
    String baseJsName = baseJs.getName();
    if ("base.js".equals(baseJsName)) {
      return "deps.js";
    } else {
      return baseJsName.replaceAll("\\/base\\.js$", "/deps.js");
    }
  }
}
