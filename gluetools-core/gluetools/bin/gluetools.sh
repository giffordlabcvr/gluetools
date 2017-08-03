#!/bin/bash

if [ -z ${GLUE_HOME+x} ]; then
    echo "Error: GLUE_HOME is not set."
    echo "Please set GLUE_HOME to the path of the GLUE home directory, e.g. in your .bash_profile"
fi


export GLUEJARS=(`ls ${GLUE_HOME}/lib/*.jar`)


if [ ${#GLUEJARS[@]} \> 1 ]; then
    echo "Multiple jar files found in ${GLUE_HOME}/lib -- please remove one"
	exit 1
fi

export GLUEJAR=${GLUEJARS[0]}

export GLUECONF=`ls ${GLUE_HOME}/conf/gluetools-config.xml`

export UNAME=`uname -s`

if [[ $UNAME == CYGWIN* ]]; then
    export GLUEJAR_WIN=`cygpath -w ${GLUEJAR}`
    export GLUECONF_WIN=`cygpath -w ${GLUECONF}`
    # this should always execute even if glue exits abnormally
    trap 'stty icanon echo' EXIT
    stty -icanon min 1 -echo
    java -Djline.terminal=jline.UnixTerminal \
	 -Dlog4j.skipJansi=true \
	 -jar "${GLUEJAR_WIN}" \
	 -c "${GLUECONF_WIN}" \
	 ${@}
else 
    java -jar $GLUEJAR \
	 -c $GLUECONF \
	 ${@}
fi
