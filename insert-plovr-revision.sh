#!/bin/bash

DIR=build/classes/revisions
cd `hg root`
mkdir -p ${DIR}
hg tip | grep changeset | awk '{print $2}' > ${DIR}/rev-plovr.txt

