package org.plovr.docgen;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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

  public DescriptorPass(AbstractCompiler compiler) {
    this.compiler = compiler;
    provides = Sets.newHashSet();
    classes = Maps.newHashMap();
  }

  @Override
  public void process(Node externs, Node root) {
    DescriptorPassCallback callback = new DescriptorPassCallback(compiler);
    NodeTraversal.traverse(compiler, root, callback);

    try {
      ImmutableMap.Builder<String, ClassDescriptor> mapBuilder =
          ImmutableMap.builder();
      for (Map.Entry<String, ClassDescriptor.Builder> entry : classes.entrySet()) {
        mapBuilder.put(entry.getKey(), entry.getValue().build());
      }
      DocWriter writer = new DocWriter(mapBuilder.build());
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
        processAssign(n);
        break;
      }
    }

    private void processAssign(Node n) {
      Node left = n.getFirstChild();
      if (left.getType() == Token.GETPROP) {
        String name = left.getQualifiedName();
        if (name == null) {
          return;
        }

        if (provides.contains(name)) {
          // Constructor or Enum
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
            }
          }
        } else if (name.contains(".prototype.")) {
          Node assigneeValue = left.getNext();
          // TODO(bolinfest): This heuristic is incomplete: in addition to
          // goog.abstractMethod, other valid values include goog.nullFunction,
          // goog.functions.TRUE, etc.
          if (assigneeValue.getType() == Token.FUNCTION ||
              (assigneeValue.getType() == Token.GETPROP &&
              "goog.abstractMethod".equals(assigneeValue.getQualifiedName()))) {
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
              addMethod(className, methodName, info);
            }
          }
        }
      }
    }

    private void addMethod(String className, String methodName, JSDocInfo info) {
      MethodDescriptor.Builder builder = MethodDescriptor.builder();
      builder.setName(methodName);

      if (info == null) {
        // TODO(bolinfest): Try to extract the parameters from the AST.
        // May not be possible if value is goog.abstractMethod.
        logger.warning(String.format(
            "No documentation for method %s.prototype.%s()",
            className,
            methodName));
        builder.setAccessLevel(AccessLevel.PUBLIC);
      } else if (info.isOverride()) {
        // If @override is present, copy the signature information from the
        // superclass. This is needed for methods such as setParentEventTarget()
        // in goog.ui.Component.
        TypeExpression superClass = classes.get(className).getSuperClass();
        String superClassName = superClass.getDisplayName();
        ClassDescriptor.Builder superBuilder = classes.get(superClassName);
        MethodDescriptor superMethodDescriptor = superBuilder.
            getInstanceMethodByName(methodName);
        if (superMethodDescriptor == null) {
          throw new RuntimeException(String.format(
              "Method %s() does not exist in %s", methodName, superClassName));
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
      } else {
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

      classes.get(className).addInstanceMethod(builder.build());
    }
  }
}
