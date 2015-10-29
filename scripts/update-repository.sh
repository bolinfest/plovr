#!/bin/bash
#
# Use this script to update the specified Closure Tool. Usage:
#
# ./update-repository.sh closure-library

cd `dirname $0`/..

# Make sure that exactly one argument is specified.
EXPECTED_ARGS=2
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Must specify one of: closure-library, closure-compiler, closure-templates and a commit hash"
  exit 1
fi

# Make sure that the argument correctly identifies a repository.
REPOSITORY=$1
if [ ! -d "closure/${REPOSITORY}" ]; then
  echo "No repository for ${REPOSITORY}"
  exit 1
fi

COMMIT=$2

set -ex
git subtree pull --prefix="closure/${REPOSITORY}" "git@github.com:google/${REPOSITORY}" "$2"
echo "$2" > tools/imports/rev-$1.txt

if [ "$REPOSITORY" = "closure-library" ]; then
  ./listfiles.sh closure/closure-library/closure/goog > library_manifest.txt
  ./listfiles.sh closure/closure-library/third_party/closure/goog > third_party_manifest.txt
fi

if [ "$REPOSITORY" = "closure-compiler" ]; then
  ./listfiles.sh closure/closure-compiler/externs > externs_manifest.txt
fi
