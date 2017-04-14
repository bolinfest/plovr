#!/bin/bash

set -ex

cd `dirname $0`
rm -f package/bin/plovr.jar
buck clean
buck fetch ...
buck build plovr
cp buck-out/gen/plovr.jar package/bin/plovr.jar
