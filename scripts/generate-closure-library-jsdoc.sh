#!/bin/bash

set -e

cd `hg root`

if [ ! -e "build/plovr.jar" ]; then
  echo "Must build plovr.jar before running this script"
  exit 1
fi

OUTPUT_DIR=build/www/jsdoc
mkdir -p $OUTPUT_DIR

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

echo $CONFIG > $OUTPUT_DIR/config.js

java -Xmx512m -jar build/plovr.jar jsdoc $OUTPUT_DIR/config.js
