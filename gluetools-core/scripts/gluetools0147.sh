#!/bin/bash
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar ${GLUETOOLS_HOME}/build/libs/gluetools-core-all-0.1.47.jar -c ${GLUETOOLS_HOME}/gluetools-config-0147.xml "$@"
