#!/bin/bash
#
# Use this script to update the specified Closure Tool. Usage:
#
# ./update-repository.sh closure-library

cd `dirname $0`/..

# Make sure that exactly one argument is specified.
EXPECTED_ARGS=1
if [ $# -ne $EXPECTED_ARGS ]; then
  echo "Must specify one of: closure-library, closure-compiler, closure-templates"
  exit 1
fi

# Make sure that the argument correctly identifies a repository.
REPOSITORY=$1
if [ ! -d "closure/${REPOSITORY}" ]; then
  echo "No repository for ${REPOSITORY}"
  exit 1
fi

set -ex
git subtree pull --prefix="closure/${REPOSITORY}" "git@github.com:google/${REPOSITORY}" master
