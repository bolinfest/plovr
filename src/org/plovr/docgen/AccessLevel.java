package org.plovr.docgen;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;

public enum AccessLevel {
  PUBLIC,
  PROTECTED,
  PRIVATE,
  ;

  /**
   * className is only used if the JSDocInfo is for an instance method.
   */
  public static AccessLevel getLevelForInfo(
      JSDocInfo info,
      String className,
      String methodName,
      Map<String, ClassDescriptor.Builder> classes) {
    if (info == null) {
      return PUBLIC;
    } else {
      Visibility visibility = info.getVisibility();
      if (visibility == null) {
        return PUBLIC;
      }
      switch (visibility) {
        case PUBLIC: return PUBLIC;
        case PROTECTED: return PROTECTED;
        case PRIVATE: return PRIVATE;
        case INHERITED:
          if (className == null) {
            return PUBLIC;
          } else {
            return getSuperLevel(className, methodName, classes);
          }
        default:
          throw new RuntimeException("Unknown visibility: " + visibility);
      }
    }
  }

  private static AccessLevel getSuperLevel(
      String className,
      String methodName,
      Map<String, ClassDescriptor.Builder> classes) {
    ClassDescriptor.Builder builder = classes.get(className);
    TypeExpression superClass = builder.getSuperClass();
    if (superClass == null) {
      return PUBLIC;
    }

    String superClassName = superClass.getDisplayName();
    ClassDescriptor.Builder superBuilder = classes.get(superClassName);
    Preconditions.checkNotNull(superBuilder, "No builder for " + superClassName);
    MethodDescriptor method = superBuilder.getInstanceMethodByName(methodName);
    return (method == null) ? PUBLIC : method.getAccessLevel();
  }
}
