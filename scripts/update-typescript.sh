#!/bin/bash

# This updates the copy of typescript.js in the plovr repo.
# This takes the path to the typescript repository as an argument.
# Usage:
#
#     ./scripts/update-typescript.sh ../typescript
#
# Writes src/org/plovr/typescript.js

set -e

# Find the root of the plovr repository.
cd "$(git rev-parse --show-toplevel)"
PLOVR_DIR="$PWD"

# Build typescript.js.
pushd "$1"
make compiler

# Copy the result to the plovr repository.
cp built/local/typescript.js $PLOVR_DIR/src/org/plovr/
cp built/local/lib.d.ts      $PLOVR_DIR/src/org/plovr/

# Achieve balance.
popd
