# Have been bitten by this before?
# Though maybe it is part of the problem?
# ant clean jar

# This will rebuild plovr.jar and serve the docs on plovr.com.
ant serve-prod-documentation &
sleep 20s

# This will run the plovr demo.
ant run-demo &

