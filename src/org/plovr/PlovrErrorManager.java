package org.plovr;

import com.google.common.collect.Maps;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.PrintStreamErrorManager;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class PlovrErrorManager extends PrintStreamErrorManager {
  private final Set<Pattern> warningExcludePaths;
  private Config config;
  private Map<Pattern,Integer> excludeCounts = Maps.newHashMap();
  private int excludedWarningCount = 0;

  public PlovrErrorManager(Config config) {
    super(config.getErrorStream());
    this.config = config;
    this.warningExcludePaths = config.getWarningExcludePaths();
  }

  @Override
  public void println(CheckLevel level, JSError error) {
    String sourceName = error.sourceName;
    boolean exclude = false;
    for(Pattern warningExcludePath: this.warningExcludePaths) {
      if(warningExcludePath.matcher(sourceName).matches()) {
        exclude = true;
        excludedWarningCount++;
        break;
      }
    }
    if(!exclude) {
      super.println(level, error);
    }
  }

  public int getExcludedWarningCount() {
    return excludedWarningCount;
  }

  @Override
  public int getWarningCount() {
    return super.getWarningCount() - this.getExcludedWarningCount();
  }

  @Override
  public void printSummary() {
    PrintStream stream = config.getErrorStream();
    stream.format("%d error(s), %d warning(s), %d excluded warning(s), %.1f%% typed%n",
        getErrorCount(), getWarningCount(), getExcludedWarningCount(), getTypedPercent());
  }
}
