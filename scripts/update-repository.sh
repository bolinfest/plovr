#!/bin/bash
#
# Use this script to update closure-library. Usage:
#
# ./update-repository.sh hash

cd `dirname $0`/..

# Make sure that exactly one argument is specified.
EXPECTED_ARGS=1
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Must specify a commit hash"
  exit 1
fi

REPOSITORY="closure-library"
COMMIT=$2

set -ex
git subtree pull --prefix="closure/${REPOSITORY}" "git@github.com:google/${REPOSITORY}" "$2"
echo "$2" > tools/imports/rev-$1.txt

./listfiles.sh closure/closure-library/closure/goog > library_manifest.txt
./listfiles.sh closure/closure-library/third_party/closure/goog > third_party_manifest.txt
