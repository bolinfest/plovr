#!/bin/sh

SVNREPO=${SVNREPO:-http://closure-templates.googlecode.com/svn/}
BRANCH="closure-templates"

HG=${HG:-hg}
HGROOT=${HGROOT:-$(${HG} root)}

${HG} convert \
  --branchmap ${HGROOT}/tools/imports/${BRANCH}/branchmap \
  --filemap ${HGROOT}/tools/imports/${BRANCH}/filemap \
  ${SVNREPO} \
  ${HGROOT} \
  ${HGROOT}/tools/imports/${BRANCH}/shamap
