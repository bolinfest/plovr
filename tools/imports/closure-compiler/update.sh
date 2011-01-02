#!/bin/sh

SVNREPO=${SVNREPO:-http://closure-compiler.googlecode.com/svn/}
BRANCH="closure-compiler"

HG=${HG:-hg}
HGROOT=${HGROOT:-$(${HG} root)}

${HG} convert \
  --branchmap ${HGROOT}/tools/imports/${BRANCH}/branchmap \
  --filemap ${HGROOT}/tools/imports/${BRANCH}/filemap \
  ${SVNREPO} \
  ${HGROOT} \
  ${HGROOT}/tools/imports/${BRANCH}/shamap
