#!/bin/bash

# Generates a JAR of .java files for the .jj files.

JAVACC="$1"
GSS_PARSER__JJ="$2"
OUTPUT_FILE="$3"
TMP="$4"

cd $TMP

mkdir -p com/google/common/css/compiler/ast
java -classpath $JAVACC org.javacc.parser.Main $GSS_PARSER__JJ &> /dev/null
mv *.java com/google/common/css/compiler/ast

zip -r $OUTPUT_FILE *
