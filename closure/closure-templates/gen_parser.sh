#!/bin/bash

# Generates a JAR of .java files for the .jj files.

JAVACC="$1"
EXPRESSION_PARSER_JJ="$2"
SOY_FILE_PARSER_JJ="$3"
TYPE_PARSER_JJ="$4"
OUTPUT_FILE="$5"
TMP="$6"

cd $TMP

mkdir -p com/google/template/soy/exprparse
java -classpath $JAVACC org.javacc.parser.Main $EXPRESSION_PARSER_JJ &> /dev/null
mv *.java com/google/template/soy/exprparse

mkdir -p com/google/template/soy/soyparse
java -classpath $JAVACC org.javacc.parser.Main $SOY_FILE_PARSER_JJ &> /dev/null
mv *.java com/google/template/soy/soyparse

mkdir -p com/google/template/soy/types/parse
java -classpath $JAVACC org.javacc.parser.Main $TYPE_PARSER_JJ &> /dev/null
mv *.java com/google/template/soy/types/parse

zip -r $OUTPUT_FILE *
