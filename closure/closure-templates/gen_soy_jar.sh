#!/bin/bash

# Generates the compiled Soy JAR file.

set -ex

OUTPUT_FILE="$1"
PARSER_SRC_JAR="$3"
JAVA_SRCS=`echo "$@" | cut -d ' ' -f 4-`

# Copy all of the .java files to $TMP.
tar cf $TMP/src.tar $JAVA_SRCS
cd $TMP
tar xvf src.tar
rm src.tar
cp $PARSER_SRC_JAR .
tar xf parser.src.tar
rm parser.src.tar

cd -
CLASSPATH=`buck audit classpath //closure/closure-templates:classpath_hack | xargs | sed -e 's/ /:/g'`

mkdir $TMP/classes
find $TMP -name \*.java | xargs javac -classpath $CLASSPATH -d $TMP/classes
cd $TMP/classes
jar cf $OUTPUT_FILE *
