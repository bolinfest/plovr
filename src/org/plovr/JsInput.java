package org.plovr;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.SourceFile.Generator;

/**
 * {@link JsInput} represents a JavaScript input to the Closure Compiler.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public interface JsInput extends Generator {

  /**
   * @return name of a {@link JsInput} must be unique among the other inputs
   * included in a compilation so that warnings and errors can be reported
   * appropriately.
   */
  public String getName();

  /**
   * @return JavaScript code
   */
  public String getCode();

  /**
   * @return a list of the values passed to goog.provide() in this input
   */
  public List<String> getProvides();

  /**
   * @return a list of the values passed to goog.require() in this input
   */
  public List<String> getRequires();

  /**
   * @return a boolean indicating whether this is a Soy file. If true, then
   * {@link #getTemplateCode} must return the original Soy content.
   */
  public boolean isSoyFile();

  /**
   * @return the underlying template code if this is a Soy file
   * @throws UnsupportedOperationException if this is not a Soy file
   */
  public String getTemplateCode();

  /**
   * Whether this input can calculate a stable ETag value for itself.
   * @see #getCodeWithEtag()
   */
  public boolean supportsEtags();

  /**
   * If {@link #supportsEtags()} returns {@code true}, then this returns the
   * value returned by {@link #getCode()} along with an ETag; otherwise, it
   * throws an {@link UnsupportedOperationException}.
   * <p>
   * This is generally used as a performance optimization to prevent plovr from
   * going to disk twice: once to read the code for {@link #getCode()} and then
   * again to read the code and calculate its ETag. This ensures that the code
   * and ETag are produced atomically.
   */
  public CodeWithEtag getCodeWithEtag();

  static final class CodeWithEtag {
    public final String code;
    public final String eTag;
    public CodeWithEtag(String code, String eTag) {
      Preconditions.checkNotNull(code);
      Preconditions.checkNotNull(eTag);
      this.code = code;
      this.eTag = eTag;
    }
  }
}
