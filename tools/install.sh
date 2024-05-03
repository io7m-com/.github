#!/bin/sh -ex

mvn clean package

cp target/com.io7m.ghtools-*-main.jar "${HOME}/bin/com.io7m.ghtools.jar"

chmod 755 ghtools
chmod 755 ghtools-debug

cp ghtools "${HOME}/bin/"
cp ghtools-debug "${HOME}/bin"

