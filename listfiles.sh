# Creating a separate script to list files in a directory is much easier than
# trying to accomplish the equivalent thing in pure Ant.

# Change to the directory to eliminate any relative directory prefix.
cd $1 > /dev/null

# Remove the "./" from the beginning of each entry, and exclude deps.js/test files.
find . -name '*.js' | grep -v deps.js | grep -v _test.js | cut -b 3-
