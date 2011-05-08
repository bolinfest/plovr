package org.plovr.docgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;

public class DocWriter {

  private static SoyTofu tofu;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(DocWriter.class, "docgen.soy"));
    SoyFileSet fileSet = builder.build();
    tofu = fileSet.compileToJavaObj();
  }

  private final Map<String, ClassDescriptor> classes;

  private final File documentationRootDirectory;

  public DocWriter(Map<String, ClassDescriptor> classes) {
    this.classes = classes;

    // TODO(bolinfest): Make this configurable.
    documentationRootDirectory = new File("build/docgen");
    documentationRootDirectory.mkdirs();
  }

  public void write() throws IOException {
    Set<String> paths = Sets.newTreeSet();

    for (Map.Entry<String, ClassDescriptor> entry : classes.entrySet()) {
      String className = entry.getKey();
      String path = classNameToPath(className);
      paths.add(path);
      File file = new File(documentationRootDirectory, path);
      Files.createParentDirs(file);
      file.createNewFile();
      Writer writer = new FileWriter(file);

      ClassDescriptor descriptor = entry.getValue();
      String pathToBase = createPathToBase(className);
      String pathToSuper;
      if (descriptor.getSuperClass() == null) {
        pathToSuper = null;
      } else {
        pathToSuper = classNameToPath(descriptor.getSuperClass().getDisplayName());
      }
      writer.append(tofu.render(
          "plovr.docgen.classPage",
          new SoyMapData(
              "pathToBase", pathToBase,
              "classDescriptor", descriptor.toSoyData(),
              "pathToSuper", pathToSuper),
          null /* msgBundle */));
      writer.close();
    }

    writeIndex(paths);
  }

  private static String classNameToPath(String className) {
    return className.replace('.', '/') + ".html";
  }

  private static String createPathToBase(String className) {
    String[] parts = className.split("\\.");
    return Strings.repeat("../", parts.length - 1);
  }

  private void writeIndex(Set<String> paths) throws IOException {
    File index = new File(documentationRootDirectory, "index.html");
    Writer writer = new FileWriter(index);
    writer.append(tofu.render(
        "plovr.docgen.index",
        new SoyMapData("hrefs", new SoyListData(paths)),
        null /* msgBundle */));
    writer.close();
  }
}
