#!/bin/bash
#
# Use this script to update closure-library. Usage:
#
# ./update-closure-library.sh hash

cd `dirname $0`/..

# Make sure that exactly one argument is specified.
EXPECTED_ARGS=1
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Must specify a commit hash"
  exit 1
fi

REPOSITORY="closure-library"
COMMIT=$1

set -ex
git subtree pull --prefix="closure/${REPOSITORY}" "git@github.com:google/${REPOSITORY}" "$COMMIT"
echo "$COMMIT" > tools/imports/rev-$REPOSITORY.txt

./listfiles.sh closure/closure-library/closure/goog | sort > library_manifest.txt
./listfiles.sh closure/closure-library/third_party/closure/goog | sort > third_party_manifest.txt
