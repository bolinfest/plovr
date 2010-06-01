ant release
VERSION=`hg identify | cut -f1 -d ' '`
cp build/plovr.jar build/plovr-$VERSION.jar
