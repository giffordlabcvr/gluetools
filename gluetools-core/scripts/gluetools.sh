#!/bin/bash

remoteDebug="true"
verboseGC="true"
localConfig="false"
remargs=()

for d in "$@"
do
if [ "${d}" == "-l" ]
then
    localConfig="true"
elif [ "${d}" == "-d" ]
then
    remoteDebug="false"
else
    remargs=("${remargs[@]}" "${d}") # push element 'd'
fi
done

export GLUETOOLS_VERSION=`(cd ${GLUETOOLS_HOME}/.. ; ./getCurrentVersion.sh)`
export GLUETOOLS_CONFIG_XML=${GLUETOOLS_HOME}/gluetools-config.xml
if [ $localConfig == "true" ]
then
if [ -e "local-gluetools-config.xml" ]
then
    echo "Using config local-gluetools-config.xml"
    export GLUETOOLS_CONFIG_XML="local-gluetools-config.xml"
else
    echo "local-gluetools-config.xml does not exist"
    exit 1 
fi
fi

export GLUE_DEBUG_OPTS=""
if [ $remoteDebug == "true" ]
then
    echo "Remote debug switched on: use -d to suppress this."
    export GLUE_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n"
fi

export GLUE_VERBOSE_GC_OPTS=""
if [ $verboseGC == "true" ]
then
    echo "Verbose GC switched on."
    export GLUE_VERBOSE_GC_OPTS="-XX:+PrintGC"
fi

(cd ${GLUETOOLS_HOME} ; gradle --quiet jar)
java ${GLUE_DEBUG_OPTS} ${GLUE_VERBOSE_GC_OPTS} -jar ${GLUETOOLS_HOME}/build/libs/gluetools-core-${GLUETOOLS_VERSION}.jar -c ${GLUETOOLS_CONFIG_XML} "${remargs[@]}"
