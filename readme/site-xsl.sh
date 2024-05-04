#!/bin/sh -ex

if [ -z "${SITE_XML}" ]
then
  echo "SITE_XML is undefined" 1>&2
  exit 1
fi

./saxon.sh -xi -xsl:site.xsl -s:${SITE_XML}
