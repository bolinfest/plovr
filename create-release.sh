ant jar
VERSION=`git rev-parse --short HEAD`
if ! git diff-index --quiet HEAD 2> /dev/null ; then
  VERSION=$VERSION-dirty
fi
cp build/plovr.jar build/plovr-$VERSION.jar
chmod 755 build/plovr.jar
