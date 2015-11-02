# Run this script and then load test-build.html to see the modules in action.

set -ex

cd `dirname $0`/../..
rm -rf build/module-example/
buck build plovr
java -jar buck-out/gen/plovr.jar build testdata/modules/plovr-config.js --create_source_map=build/module-example/
