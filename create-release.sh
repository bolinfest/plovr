ant jar
VERSION=`hg identify | cut -f1 -d ' '`
cp build/plovr.jar build/plovr-$VERSION.jar
chmod 755 build/plovr.jar
