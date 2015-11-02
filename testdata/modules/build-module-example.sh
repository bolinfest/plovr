# Run this script and then load test-build.html to see the modules in action.

cd ../..
rm -rf build/module-example/
buck build plovr
java -jar buck-out/gen/plovr.jar build testdata/modules/plovr-config.js
