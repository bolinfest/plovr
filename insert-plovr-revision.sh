#!/bin/bash

DIR=build/classes/revisions
cd "$(git rev-parse --show-toplevel)"
mkdir -p ${DIR}
git rev-parse HEAD > ${DIR}/rev-plovr.txt
