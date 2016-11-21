#!/bin/bash

options=':l'
localConfig='false'
while getopts $options option
do
    case $option in
        l  )    localConfig='true';;
        \? )    if (( (err & ERROPTS) != ERROPTS ))
                then
                    error $NOEXIT $ERROPTS "Unknown option."
                fi;;
        *  )    echo "Missing option argument."
		exit 1;;
    esac
done

shift $(($OPTIND - 1))

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
(cd ${GLUETOOLS_HOME} ; gradle --quiet jarAll)
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar ${GLUETOOLS_HOME}/build/libs/gluetools-core-all-${GLUETOOLS_VERSION}.jar -c ${GLUETOOLS_CONFIG_XML} "$@"
