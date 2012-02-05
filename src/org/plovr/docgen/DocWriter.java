package org.plovr.docgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;

public class DocWriter {

  private static final SoyTofu tofu;
  private static final URL stylesheet;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(DocWriter.class, "docgen.soy"));
    SoyFileSet fileSet = builder.build();
    tofu = fileSet.compileToTofu();

    stylesheet = Resources.getResource(DocWriter.class, "stylesheet.css");
  }

  private final Map<String, ClassDescriptor> classes;

  private final Map<String, LibraryDescriptor> libraries;

  private final File documentationRootDirectory;

  private final Set<String> paths = Sets.newTreeSet();

  public DocWriter(
      File documentationRootDirectory,
      Map<String, ClassDescriptor> classes,
      Map<String, LibraryDescriptor> libraries) {
    this.classes = classes;
    this.libraries = libraries;
    this.documentationRootDirectory = documentationRootDirectory;
    documentationRootDirectory.mkdirs();
  }

  public void write() throws IOException {

    for (Map.Entry<String, ClassDescriptor> entry : classes.entrySet()) {
      String className = entry.getKey();
      Writer writer = createWriterForProvide(className);

      ClassDescriptor descriptor = entry.getValue();
      String pathToBase = createPathToBase(className);
      String pathToSuper;
      if (descriptor.getSuperClass() == null) {
        pathToSuper = null;
      } else {
        pathToSuper = classNameToPath(descriptor.getSuperClass().getDisplayName());
      }
      writer.append(tofu.newRenderer("plovr.docgen.classPage").setData(
          new SoyMapData(
              "pathToBase", pathToBase,
              "classDescriptor", descriptor.toSoyData(),
              "pathToSuper", pathToSuper)).
          render());
      writer.close();
    }

    for (Map.Entry<String, LibraryDescriptor> entry : libraries.entrySet()) {
      String libraryName = entry.getKey();
      Writer writer = createWriterForProvide(libraryName);

      LibraryDescriptor descriptor = entry.getValue();
      String pathToBase = createPathToBase(libraryName);
      writer.append(tofu.newRenderer("plovr.docgen.libraryPage").setData(
          new SoyMapData(
              "pathToBase", pathToBase,
              "libraryDescriptor", descriptor.toSoyData())).
          render());
      writer.close();
    }

    writeIndex(paths);
    writeStylesheet();
  }

  private Writer createWriterForProvide(String provide) throws IOException {
    String path = classNameToPath(provide);
    paths.add(path);
    File file = new File(documentationRootDirectory, path);
    Files.createParentDirs(file);
    file.createNewFile();
    return new FileWriter(file);
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
    writer.append(tofu.newRenderer("plovr.docgen.index").
        setData(new SoyMapData("hrefs", new SoyListData(paths))).
        render());
    writer.close();
  }

  private void writeStylesheet() throws IOException {
    InputSupplier<? extends InputStream> from = new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        return stylesheet.openStream();
      }
    };
    File to = new File(documentationRootDirectory, "stylesheet.css");
    Files.copy(from, to);
  }
}
