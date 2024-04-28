#!/bin/sh

SAXON_JARS="
Saxon-HE-11.4.jar
xml-apis-1.4.01.jar
xmlresolver-4.4.3-data.jar
xmlresolver-4.4.3.jar
"

SAXON_CLASSPATH=""

for JAR in ${SAXON_JARS}
do
  REAL_JAR=$(realpath "saxon/${JAR}") || exit 1
  SAXON_CLASSPATH="${SAXON_CLASSPATH}:${REAL_JAR}"
done

exec java -cp "${SAXON_CLASSPATH}" net.sf.saxon.Transform "$@"
