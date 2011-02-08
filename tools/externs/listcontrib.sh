#!/bin/bash
# Script to copy the user-contributed externs from the Closure Compiler to
# plovr.
#
# Usage:
# listcontrib.sh <contrib-externs-dir> <target-dir>

# Find all the files under the closure-compiler/contrib/externs directory and
# copy them to a directory under build/
find $1 -name '*.js' -exec cp '{}' $2 \;

# Change to that directory and list its contents to create a manifest.
cd $2
find . -name '*.js' | sort | cut -b 3-
