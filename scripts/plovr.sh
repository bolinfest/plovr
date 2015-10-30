#!/bin/bash

# Script that is designed to run plovr "unpacked."
#
# IMPORTANT: `buick build plovr` must be run once before running this script.
#
# Running `buck build plovr` does three important things:
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
cd "$(git rev-parse --show-toplevel)"
PLOVR_DIR="$PWD"

# Navigate back to the original directory and run plovr.
cd $ORIGINAL_PWD

# Run plovr, specifying its long classpath.
# Including directories in the classpath that contain static resources,
# such as .js files, ensures that they can be loaded as Resources from
# Java code.
java \
-classpath \
${PLOVR_DIR}/buck-out/gen/plovr.jar \
org.plovr.cli.Main "$@"
