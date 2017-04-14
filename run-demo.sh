# Have been bitten by this before?
# Though maybe it is part of the problem?
# ant clean jar

# This will run the plovr demo.
buck fetch ...
buck build plovr
java -classpath buck-out/gen/plovr.jar org.plovr.cli.Main serve www/demo/demo-config.js testdata/modules/plovr-config.js

# This was used to debug http://code.google.com/p/chromium/issues/detail?id=105824
# PLOVR_JAR=/www/bolinfest.com/plovr/plovr-with-etags.jar
# java -jar -Xmx128m $PLOVR_JAR serve www/demo/demo-config.js \
#     testdata/modules/plovr-config.js &
