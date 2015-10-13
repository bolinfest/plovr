# Creating a separate script to list files in a directory is much easier than
# trying to accomplish the equivalent thing in pure Ant.

# Change to the directory to eliminate any relative directory prefix.
cd $1

# Remove the "./" from the beginning of each entry, and exclude deps.js files.
find . -name '*.js' | grep -v deps.js | cut -b 3-
