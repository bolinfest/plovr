#!/bin/bash

set -e
cd `hg root`

ant clean jar

# Directory where content should go while it is being created.
WWW_DIR=/tmp/plovr.com/
rm -rf $WWW_DIR
mkdir $WWW_DIR

# Create the JSDoc for the Closure Library.
./scripts/generate-closure-library-jsdoc.sh

# Build the prod documentation and start SoyWeb on port 9811.
ant serve-prod-documentation &

# Wait for SoyWeb to start.
# This can take a long time because there is a lot to of content
# to generate before the server starts up.
# We make the sleep interval extremely conservative to account for this.
echo "Waiting 60 seconds for SoyWeb to start..."
sleep 60

# Copy each translated Soy file to $WWW_DIR.
for NAME in `find www -name '*.soy' | xargs -I {} echo {} | sed -e 's#www/##' | sed -e 's#.soy#.html#'`
do
  # Ignore common.soy.
  if [ "$NAME" != "__common.html" -a "$NAME" != "demo/example/templates.soy" ]; then
    echo "Copying $NAME"
    DEST=${WWW_DIR}${NAME}
    mkdir -p `dirname $DEST`
    curl "http://127.0.0.1:9811/$NAME" > "$DEST"
  fi
done

# Kill SoyWeb.
fuser -k -n tcp 9811

# Copy all static files.
pushd build/www
cp -r * $WWW_DIR
popd

# Fix all the file permissions, just in case.
find $WWW_DIR -type d | xargs chmod 755
find $WWW_DIR -type f | xargs chmod 644

# Swap out the existing documentation with the new documentation.
rm -rf /www/plovr.com/
mv $WWW_DIR /www/plovr.com/
echo "SUCCESS"
