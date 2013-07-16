#!/bin/bash

# Script that is designed to run plovr "unpacked."
#
# IMPORTANT: `ant compile` must be run once before running this script.
#
# Running `ant compile` does three important things:
#
# (1) It generates files, such as the .java files generated from Soy's grammar.
# (2) It copies static resources, such as externs, to the place where Ant
#     later expects them so it can put them in a JAR file.
# (3) It compiles .java files to .class files.
#
# In the steady-state of plovr development, the inputs to (1) and (2) do not
# change very often, so it is wasteful to do them every time `ant compile` is
# run. Unfortunately, Ant does not know any better, so it always performs
# these expensive steps.
#
# The main thing that we are interested in is modifying .java files and then
# re-running plovr with these new changes. Eclipse is configured to write
# its .class files to build/classes, just as Ant is, so its incremental
# compilation will generate .class files to the right place.
#
# From here, we can run "unpacked," i.e., not from plovr.jar, so long as
# the .class files in build/classes/ can read resources from disk just as
# they could if they were in plovr.jar. In this way, for most changes to
# plovr, we can modify code in Eclipse and then re-run this script, which
# is much faster than invoking `ant jar` between each change.

set -e

# Record the original working directory.
ORIGINAL_PWD="$PWD"

# Navigate to the directory that contains this script.
# From http://stackoverflow.com/a/246128/396304
SOURCE="${BASH_SOURCE[0]}"
PLOVR_SCRIPT_DIR="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do 
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$PLOVR_SCRIPT_DIR/$SOURCE"
  PLOVR_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
PLOVR_SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
cd $PLOVR_SCRIPT_DIR

# Find the absolute path of the plovr project directory. 
cd `hg root`
PLOVR_DIR="$PWD"

# Navigate back to the original directory and run plovr.
cd $ORIGINAL_PWD

# Run plovr, specifying its long classpath.
# Including directories in the classpath that contain static resources,
# such as .js files, ensures that they can be loaded as Resources from
# Java code.
java \
-classpath \
${PLOVR_DIR}/build/classes:\
${PLOVR_DIR}/lib/guava-14.0.1.jar:\
${PLOVR_DIR}/lib/gson-2.2.2.jar:\
${PLOVR_DIR}/lib/junit-4.8.2.jar:\
${PLOVR_DIR}/lib/selenium-java-2.21.0.jar:\
${PLOVR_DIR}/closure/closure-compiler/lib/args4j.jar:\
${PLOVR_DIR}/closure/closure-compiler/lib/json.jar:\
${PLOVR_DIR}/closure/closure-compiler/lib/jsr305.jar:\
${PLOVR_DIR}/closure/closure-compiler/lib/protobuf-java.jar:\
${PLOVR_DIR}/closure/closure-compiler/build/lib/rhino.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/aopalliance.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/guice-3.0.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/guice-assistedinject-3.0.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/guice-multibindings-3.0.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/icu4j-core.jar:\
${PLOVR_DIR}/closure/closure-templates/java/lib/javax.inject.jar:\
${PLOVR_DIR}/build/soy-resources/:\
${PLOVR_DIR}/closure/closure-compiler/:\
${PLOVR_DIR}/closure/closure-templates/javascript/:\
${PLOVR_DIR}/closure/closure-library/:\
${PLOVR_DIR}/lib/closure-stylesheets-20130106.jar \
org.plovr.cli.Main "$@"
