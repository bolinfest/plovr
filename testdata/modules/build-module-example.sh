# Run this script and then load test-build.html to see the modules in action.

cd ../..
rm -rf build/module-example/
ant jar
java -jar build/plovr.jar build testdata/modules/plovr-config.js
