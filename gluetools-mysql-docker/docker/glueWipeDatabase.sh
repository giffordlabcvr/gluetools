#!/bin/bash

export GLUETOOLS_USER=gluetools
export GLUETOOLS_DB=GLUE_TOOLS

echo "Deleting old GLUE database $GLUETOOLS_DB ..."
echo "drop database $GLUETOOLS_DB;" | mysql --user=${GLUETOOLS_USER} --password=glue12345 2> /dev/null
echo "Creating new GLUE database $GLUETOOLS_DB ..."
echo "create database $GLUETOOLS_DB character set UTF8;" | mysql --user=${GLUETOOLS_USER} --password=glue12345
