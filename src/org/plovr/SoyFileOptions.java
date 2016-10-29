package org.plovr;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.msgs.SoyMsgBundle;

/**
 * {@link SoyFileOptions} specifies the options to use when translating a Soy
 * file to JavaScript.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class SoyFileOptions {

  final List<String> pluginModuleNames;
  final boolean useClosureLibrary;
  final SoyMsgBundle msgBundle;

  public SoyFileOptions() {
    this(ImmutableList.<String>of(), /* pluginModuleNames */
        true); /* useClosureLibrary */
  }

  public SoyFileOptions(List<String> pluginModuleNames,
      boolean useClosureLibrary) {
    this(pluginModuleNames, useClosureLibrary, null);
  }

  private SoyFileOptions(List<String> pluginModuleNames,
      boolean useClosureLibrary,
      SoyMsgBundle msgBundle) {
    Preconditions.checkNotNull(pluginModuleNames);
    this.pluginModuleNames = ImmutableList.copyOf(pluginModuleNames);
    this.useClosureLibrary = useClosureLibrary;
    this.msgBundle = msgBundle;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        pluginModuleNames, useClosureLibrary, msgBundle);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof SoyFileOptions)) {
      return false;
    }
    SoyFileOptions that = (SoyFileOptions)obj;
    return Objects.equal(this.pluginModuleNames, that.pluginModuleNames) &&
        Objects.equal(this.useClosureLibrary, that.useClosureLibrary) &&
        Objects.equal(this.msgBundle, that.msgBundle);
  }

  public static class Builder {
    List<String> pluginModuleNames = ImmutableList.<String>of();
    boolean useClosureLibrary = false;
    SoyMsgBundle msgBundle = null;

    public Builder setPluginModuleNames(List<String> values) {
      pluginModuleNames = values;
      return this;
    }

    public Builder setUseClosureLibrary(boolean value) {
      useClosureLibrary = value;
      return this;
    }

    public Builder setMsgBundle(SoyMsgBundle value) {
      msgBundle = value;
      return this;
    }

    public SoyFileOptions build() {
      return new SoyFileOptions(
          pluginModuleNames,
          useClosureLibrary,
          msgBundle);
    }
  }
}
