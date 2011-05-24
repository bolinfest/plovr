package org.plovr.cli;

import java.io.IOException;

import org.plovr.util.VersionUtil;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;


public class InfoCommand extends AbstractCommandRunner<InfoCommandOptions> {

  @Override
  InfoCommandOptions createOptions() {
    return new InfoCommandOptions();
  }

  @Override
  String getUsageIntro() {
    return "Display the versions of the Closure Tools packaged with plovr";
  }

  @Override
  int runCommandWithOptions(InfoCommandOptions options) throws IOException {
    String libraryRevision = VersionUtil.getRevision("closure-library");
    String compilerRevision = VersionUtil.getRevision("closure-compiler");
    String templatesRevision = VersionUtil.getRevision("closure-templates");
    String plovrRevision = VersionUtil.getRevision("plovr");

    // Calculate the max length so that all versions are right-aligned (except
    // for plovr because its Hg hash is so long).
    int max = Ints.max(
        libraryRevision.length(),
        compilerRevision.length(),
        templatesRevision.length());

    System.out.println("plovr built from revision " + plovrRevision);
    System.out.println("Revision numbers for embedded Closure Tools:");
    System.out.println("Closure Library:    " +
        Strings.repeat(" ", max - libraryRevision.length()) + libraryRevision);
    System.out.println("Closure Compiler:   " +
        Strings.repeat(" ", max - compilerRevision.length()) + compilerRevision);
    System.out.println("Closure Templates:  " +
        Strings.repeat(" ", max - templatesRevision.length()) + templatesRevision);
    return 0;
  }
}
