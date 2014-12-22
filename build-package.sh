#!/bin/bash

set -ex

cd `dirname $0`
rm -f package/bin/plovr.jar
ant clean
ant jar
cp build/plovr.jar package/bin/plovr.jar
