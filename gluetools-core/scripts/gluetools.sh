#!/bin/bash
export GLUETOOLS_VERSION=`(cd ${GLUETOOLS_HOME}/.. ; ./getCurrentVersion.sh)`
(cd ${GLUETOOLS_HOME} ; gradle --quiet jarAll)
java -jar ${GLUETOOLS_HOME}/build/libs/gluetools-core-all-${GLUETOOLS_VERSION}.jar -c ${GLUETOOLS_HOME}/gluetools-config.xml "$@"
