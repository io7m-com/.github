#!/bin/sh

rm -rf image
mkdir -p image

cp ../../tools/ghtools image/ghtools
cp ../../tools/target/com.io7m.ghtools-0.0.1-SNAPSHOT-main.jar image/com.io7m.ghtools.jar

podman build \
--iidfile image-id.txt \
-t "io7m/ghtools:1.0.0" .
