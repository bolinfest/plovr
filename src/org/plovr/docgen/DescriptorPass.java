package org.plovr.docgen;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.plovr.Config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class DescriptorPass implements CompilerPass {

  private final static Logger logger = Logger.getLogger(DescriptorPass.class
      .getName());

  private final AbstractCompiler compiler;

  private final File documentationDirectory;

  /**
   * The set of string arguments passed to goog.provide() whose type has not
   * been determined. A value will be one of:
   * <ul>
   *   <li>ClassDescriptor
   *   <li>EnumDescriptor
   *   <li>LibraryDescriptor
   * </ul>
   */
  private final Set<String> provides;

  private final Map<String, ClassDescriptor.Builder> classes;

  private final Map<String, LibraryDescriptor.Builder> libraries;

  private final Map<String, EnumDescriptor.Builder> enums;

  public DescriptorPass(AbstractCompiler compiler, Config config) {
    this.compiler = compiler;
    File documentationDirectory = config.getDocumentationOutputDirectory();
    Preconditions.checkNotNull(documentationDirectory,
        "Must specify an output directory for the generated documentation");
    this.documentationDirectory = documentationDirectory;
    provides = Sets.newHashSet();
    classes = Maps.newHashMap();
    libraries = Maps.newHashMap();
    enums = Maps.newHashMap();
  }

  @Override
  public void process(Node externs, Node root) {
    DescriptorPassCallback callback = new DescriptorPassCallback(compiler);
    NodeTraversal.traverse(compiler, root, callback);

    try {
      ImmutableMap.Builder<String, ClassDescriptor> classMapBuilder =
          ImmutableMap.builder();
      for (Map.Entry<String, ClassDescriptor.Builder> entry : classes.entrySet()) {
        classMapBuilder.put(entry.getKey(), entry.getValue().build());
      }

      ImmutableMap.Builder<String, LibraryDescriptor> libraryMapBuilder =
          ImmutableMap.builder();
      for (Map.Entry<String, LibraryDescriptor.Builder> entry : libraries.entrySet()) {
        libraryMapBuilder.put(entry.getKey(), entry.getValue().build());
      }

      DocWriter writer = new DocWriter(
          documentationDirectory,
          classMapBuilder.build(),
          libraryMapBuilder.build());
      writer.write();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private class DescriptorPassCallback extends AbstractPostOrderCallback {

    private static final String GOOG = "goog";

    private final AbstractCompiler compiler;

    private DescriptorPassCallback(AbstractCompiler compiler) {
      this.compiler = compiler;
    }

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      switch (n.getType()) {
      case Token.CALL:
        // When encountering a function call, see if it is a call to
        // goog.provide().
        Node left = n.getFirstChild();
        if (left.getType() == Token.GETPROP) {
          Node name = left.getFirstChild();
          if (name.getType() == Token.NAME && GOOG.equals(name.getString())) {
            String googMethodName = name.getNext().getString();
            if ("provide".equals(googMethodName)) {
              Node arg = left.getNext();
              String provide = arg.getString();
              provides.add(provide);
              logger.info("PROVIDE: " + provide);
            }
          }
        }
        break;
      case Token.ASSIGN:
        if (n.getFirstChild().getType() == Token.GETPROP) {
          processAssign(n);
        }
        break;
      }
    }

    /**
     * @param n n.getType() returns Token.ASSIGN.
     */
    private void processAssign(Node n) {
      Node left = n.getFirstChild();
      String name = left.getQualifiedName();
      if (name == null) {
        return;
      }

      if (provides.contains(name)) {
        // Determine whether this is a constructor, interface, enum, or special
        // library, such as goog.dispose (a library defined as the one function
        // it contains) or goog.net.XmlHttp (a library defined as one function
        // with additional functions defined as properties that are not methods).
        provides.remove(name);
        if (left.getNext().getType() == Token.FUNCTION) {
          JSDocInfo info = NodeUtil.getFunctionInfo(left.getNext());
          if (info.isConstructor()) {
            ClassDescriptor.Builder builder = ClassDescriptor.builder();
            builder.setName(name);
            builder.setDescription(info.getBlockDescription());

            if (info.hasBaseType()) {
              builder.setSuperClass(
                  TypeExpression.builder().setType(info.getBaseType(), compiler).build());
            }

            classes.put(name, builder);
          } else if (info.isInterface()) {
            // TODO(bolinfest): Handle interface.
          } else {
            // TODO(bolinfest): Process library.
            // Note that this a funny type of library that contains either:
            // (1) one function: goog.dispose()
            // (2) one function with other functions as properties: goog.net.XmlHttp()
          }
        } else if (left.getNext().getType() == Token.OBJECTLIT) {
          JSDocInfo info = n.getJSDocInfo();
          // This appears to be an enum.
          if (info != null && info.hasEnumParameterType()) {
            EnumDescriptor.Builder builder = EnumDescriptor.builder();
            builder.setName(name);
            builder.setDescription(info.getBlockDescription());
            // TODO(bolinfest): Add enum values to builder.
            enums.put(name, builder);
          }
        }
      } else if (name.contains(".prototype.")) {
        // This appears to be an instance method or field.
        Node assigneeValue = left.getNext();
        // TODO(bolinfest): This heuristic is incomplete: in addition to
        // goog.abstractMethod, other valid values include
        // goog.partial(someFunc, someArg), goog.functions.TRUE, etc.
        if (assigneeValue.getType() == Token.FUNCTION ||
            (assigneeValue.getType() == Token.GETPROP &&
            ("goog.abstractMethod".equals(assigneeValue.getQualifiedName()) ||
            "goog.nullFunction".equals(assigneeValue.getQualifiedName())))) {
          boolean hasFunctionInfo = assigneeValue.getType() == Token.FUNCTION;

          // Instance method
          String[] parts = name.split("\\.prototype\\.");
          String className = parts[0];
          if (classes.containsKey(className)) {
            String methodName = parts[1];
            JSDocInfo info;
            if (hasFunctionInfo) {
              info = NodeUtil.getFunctionInfo(assigneeValue);
            } else {
              // If the value on the right is goog.abstractMethod, must get
              // the JSDocInfo in a different manner.
              info = n.getJSDocInfo();
            }
            TypeExpression superClass = classes.get(className).getSuperClass();
            MethodDescriptor method = createMethod(methodName, info, className, superClass);
            // TODO(bolinfest): Fix special cases so method is never null.
            if (method != null) {
              classes.get(className).addInstanceMethod(method);
            }
          }
        }
      } else {
        // This is likely a member of a library, such as goog.array.peek.
        // Drop off the last property (peek) to see if what's left (goog.array)
        // is a namespace declared using goog.provide().
        // Note that it may also be a static member of a class.
        int index = name.lastIndexOf('.');
        if (index < 0) {
          return;
        }

        String base = name.substring(0, index);
        if (classes.containsKey(base) ||
            provides.contains(base) ||
            libraries.containsKey(base)) {
          boolean isStaticClassMember = classes.containsKey(base);
          if (isStaticClassMember) {
            ClassDescriptor.Builder builder = classes.get(base);
            // Add the static method to the ClassDescriptor.
            if (left.getNext().getType() == Token.FUNCTION) {
              JSDocInfo info = NodeUtil.getFunctionInfo(left.getNext());
              String methodName = name.substring(index + 1);
              String className = base;
              TypeExpression superClass = builder.getSuperClass();
              MethodDescriptor method = createMethod(
                  methodName, info, className, superClass);
              builder.addStaticMethod(method);
            }
          } else {
            // Get or create a LibraryDescriptor and add the function to the
            // library.
            LibraryDescriptor.Builder builder = libraries.get(base);
            if (builder == null) {
              builder = LibraryDescriptor.builder();
              builder.setName(base);
              libraries.put(base, builder);
            }
            if (left.getNext().getType() == Token.FUNCTION) {
              JSDocInfo info = NodeUtil.getFunctionInfo(left.getNext());
              String methodName = name.substring(index + 1);
              MethodDescriptor method = createMethod(methodName, info);
              builder.addMethod(method);
            }
          }
        }
      }
    }

    /**
     * Use this to create a {@link MethodDescriptor} for a function.
     */
    private MethodDescriptor createMethod(
        String methodName,
        JSDocInfo info) {
      return createMethod(methodName, info, null, null);
    }

    /**
     * Use this to create a {@link MethodDescriptor} for a method associated
     * with a class.
     */
    private MethodDescriptor createMethod(
        String methodName,
        JSDocInfo info,
        @Nullable String className,
        @Nullable TypeExpression superClass) {
      MethodDescriptor.Builder builder = MethodDescriptor.builder();
      builder.setName(methodName);

      if (info == null) {
        // TODO(bolinfest): Try to extract the parameters from the AST.
        // May not be possible if value is goog.abstractMethod.
        String fullMethod;
        if (className == null) {
          fullMethod = String.format("%s", methodName);
        } else {
          fullMethod = String.format("%s.prototype.%s", className, methodName);
        }
        logger.warning(String.format(
            "No documentation for method %s()", fullMethod));
        builder.setAccessLevel(AccessLevel.PUBLIC);
      } else if (info.isOverride() && superClass != null) {
        // If @override is present, copy the signature information from the
        // superclass. This is needed for methods such as setParentEventTarget()
        // in goog.ui.Component.
        MethodDescriptor superMethodDescriptor;
        try {
          superMethodDescriptor = getSuperMethodDescriptor(
              className, methodName);
        } catch (NullPointerException e) {
          // TODO(bolinfest): Support interfaces.
          logger.severe(String.format(
              "Could not find inherited JSDoc for %s.prototype.%s(). " +
              "May inherit from an interface, which is not supported yet. " +
              "This method will not be included in the generated documentation.",
              className,
              methodName));
          // TODO(bolinfest): Fix this misleading reporting. For example,
          // goog.editor.Plugin declares a number of methods without anything
          // on the right hand side:
          //
          // /** JSDoc appears here with method signature. */
          // goog.editor.Plugin.prototype.execCommandInternal;
          //
          // Need to improve the heuristic to support this case.
          return null;
        }

        String description = Strings.nullToEmpty(info.getBlockDescription()).trim();
        if (description.isEmpty()) {
          description = superMethodDescriptor.getDescription();
        }

        // TODO(bolinfest): Copying all fields directly may not be appropriate,
        // as something may have changed in overriding. For example, getChild()
        // in goog.ui.Container returns a goog.ui.Control rather than a
        // goog.ui.Component.
        builder.setDescription(description);
        builder.setReturnType(superMethodDescriptor.getReturnType());
        builder.setAccessLevel(superMethodDescriptor.getAccessLevel());
        for (ParamDescriptor param : superMethodDescriptor.getParams()) {
          builder.addParam(param);
        }
      } else if (info.isOverride() && superClass == null) {
        // This may be a built-in method, such as toString().
        // TODO(bolinfest): Grab the information from the externs file.
        builder.setAccessLevel(AccessLevel.PUBLIC);
      } else {
        // Ordinary method that is not an override.
        builder.setDescription(info.getBlockDescription());
        AccessLevel accessLevel = AccessLevel.getLevelForInfo(
            info, className, methodName, classes);
        builder.setAccessLevel(accessLevel);

        // Extract the params. The JavaDoc for getParameterNames() claims that the
        // iteration order does not match the definition order, but they appear to
        // be added to a LinkedHashMap in JSDocInfo, which preserves the order.
        Set<String> paramNames = info.getParameterNames();
        for (String paramName : paramNames) {
          JSTypeExpression type = info.getParameterType(paramName);
          ParamDescriptor.Builder paramBuilder = ParamDescriptor.builder();
          paramBuilder.setName(paramName);
          paramBuilder.setDescription(info.getDescriptionForParameter(paramName));
          paramBuilder.setTypeExpression(
              TypeExpression.builder().setType(type, compiler).build());
          builder.addParam(paramBuilder.build());
        }

        // Add the return type.
        builder.setReturnType(
            TypeExpression.builder().setType(info.getReturnType(), compiler).build());
      }

      return builder.build();
    }

    /**
     * Finds the nearest superclass MethodDescriptor by the specified name for
     * the specified class.
     */
    private MethodDescriptor getSuperMethodDescriptor(
        String className, String methodName) {
      TypeExpression superClass = classes.get(className).getSuperClass();
      Preconditions.checkNotNull(superClass, "No superclass for " + className);
      String superClassName = superClass.getDisplayName();
      ClassDescriptor.Builder superBuilder = classes.get(superClassName);
      MethodDescriptor superMethodDescriptor = superBuilder.
          getInstanceMethodByName(methodName);
      if (superMethodDescriptor != null) {
        return superMethodDescriptor;
      } else {
        return getSuperMethodDescriptor(superClassName, methodName);
      }
    }
  }
}
