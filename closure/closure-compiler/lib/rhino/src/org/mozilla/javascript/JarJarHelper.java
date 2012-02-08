package org.mozilla.javascript;

/**
 * This class assists dynamic class name generation when the rhino
 * package has been jarjar'd.
 */
public class JarJarHelper {

    /**
     * The package name that all classes in the javascript package reside in.
     * By default, "org.mozilla.javascript".
     */
    public static final String javascriptPrefix =
        JarJarHelper.class.getPackage().getName();

    /**
     * The package name that all classes in the javascript package
     * reside in, but using / instead of . to separate the different
     * path components. By default, "org/mozilla/javascript".
     */
    public static final String javascriptPrefixSlashes =
        javascriptPrefix.replace(".", "/");
}
