package org.plovr;

import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticGroups;

/**
 * {@link DiagnosticGroupUtil} is a hack to work around the fact that not
 * everything that plovr needs from {@link DiagnosticGroups} is exposed via its
 * public API.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class DiagnosticGroupUtil {

  private static final BackdoorDiagnosticGroups backdoor =
      new BackdoorDiagnosticGroups();

  private DiagnosticGroupUtil() {}

  public static DiagnosticGroup forName(String name) {
    return backdoor.forName(name);
  }

  private static class BackdoorDiagnosticGroups extends DiagnosticGroups {
    @Override
    public DiagnosticGroup forName(String name) {
      return super.forName(name);
    }
  }
}
