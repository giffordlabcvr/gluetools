#!/bin/bash
export GLUETOOLS_VERSION=`./getCurrentVersion.sh`
gradle --quiet jarAll
java -jar build/libs/gluetools-core-all-${GLUETOOLS_VERSION}.jar -c gluetools-config.xml "$@"
