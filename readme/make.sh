#!/usr/bin/env bash

if [ -z "${SITE_XML}" ]
then
  echo "SITE_XML is undefined" 1>&2
  exit 1
fi

if [ -z "${BRANDING_HOME}" ]
then
  echo "BRANDING_HOME is undefined" 1>&2
  exit 1
fi

inkscape --export-type=png -o readme.png readme-overlay.svg || exit 1
convert readme.png readme.jpg || exit 1
mv readme.jpg ../profile/ || exit 1
rm readme.png || exit 1

./site-xsl.sh || exit 1

chmod +x table-icon-copy.sh || exit 1
mv table-icon-copy.sh ../profile || exit 1

pushd ../profile || exit 1
./table-icon-copy.sh || exit 1
rm table-icon-copy.sh || exit 1
popd || exit 1

cat README.in table.md > ../profile/README.md
rm table.md
