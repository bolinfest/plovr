#!/bin/bash

# This assumes that plovr has already been compiled.

set -e

cd `hg root`

mkdir -p build/jsdoc

find closure/closure-library -name '*.js' | \
    xargs grep -h -o -e "goog.provide\(.*\);" | \
    sed -e "s/goog.provide('\(.*\)');/goog.require('\1');/" > \
    build/jsdoc/requires.js

CONFIG=$(cat <<EOF
{
  "id": "closure-library",
  "inputs": "requires.js",
  "jsdoc-html-output-path": "."
}
EOF
)

echo $CONFIG > build/jsdoc/config.js

java -jar build/plovr.jar jsdoc build/jsdoc/config.js
