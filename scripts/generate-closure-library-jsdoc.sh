#!/bin/bash

set -e

cd `hg root`

find closure/closure-library -name '*.js' | \
    xargs grep -h -o -e "goog.provide\(.*\);" | \
    sed -e "s/goog.provide('\(.*\)');/goog.require('\1');/" > \
    build/www/jsdoc/requires.js

CONFIG=$(cat <<EOF
{
  "id": "closure-library",
  "inputs": "requires.js",
  "jsdoc-html-output-path": "."
}
EOF
)

echo $CONFIG > build/www/jsdoc/config.js
