#!/bin/bash
export GLUETOOLS_VERSION=`(cd ${GLUETOOLS_HOME}/.. ; ./getCurrentVersion.sh)`
(cd ${GLUETOOLS_HOME} ; gradle --quiet jarAll)
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar ${GLUETOOLS_HOME}/build/libs/gluetools-core-all-${GLUETOOLS_VERSION}.jar -c ${GLUETOOLS_HOME}/gluetools-config.xml "$@"
