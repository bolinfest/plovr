#!/bin/sh

SVNREPO=${SVNREPO:-http://closure-library.googlecode.com/svn/}
BRANCH="closure-library"

HG=${HG:-hg}
HGROOT=${HGROOT:-$(${HG} root)}

${HG} convert \
  --branchmap ${HGROOT}/tools/imports/${BRANCH}/branchmap \
  --filemap ${HGROOT}/tools/imports/${BRANCH}/filemap \
  ${SVNREPO} \
  ${HGROOT} \
  ${HGROOT}/tools/imports/${BRANCH}/shamap
