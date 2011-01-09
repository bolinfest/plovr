#!/bin/sh
# This script must be run from the root of the plovr directory.

# Build plovr.jar.
ant jar

# Extract all of the goog.provide() statements and
# convert them to goog.require() statements.
find closure/closure-library/closure -name '*.js' | \
    grep -v demos | \
    xargs -I {} grep -e "goog.provide\(.*\);" '{}' | \
    sed -e 's/goog.provide/goog.require/' \
    > build/all-requires.js

# Compile the Closure Library using plovr.
java -jar build/plovr.jar build tools/library-validator/config.js 2> build/closure-library-errors.js

# Display the output.
cat build/closure-library-errors.js
