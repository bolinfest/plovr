ant jar
VERSION=`git rev-parse --short HEAD`
cp build/plovr.jar build/plovr-$VERSION.jar
chmod 755 build/plovr.jar
