#!/bin/bash
# TODO(bolinfest): Make this part of the test suite to ensure that the custom
# pass is exercised and that warnings are printed out.
# Currently, the test is to manually inspect stderr to make sure that a warning
# from main.js is printed (base.js will contain warnings, as well).

ant jar
java -cp build/plovr.jar:build/classes org.plovr.cli.Main \
   build testdata/custompasses/config.json
