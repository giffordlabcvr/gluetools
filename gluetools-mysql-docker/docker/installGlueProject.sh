#!/bin/bash


DB_FILE=${1}.sql.gz

case "${1}" in
"hcv_glue" | "ncbi_hcv_glue")
    URL_PREFIX=http://hcv-glue.cvr.gla.ac.uk/hcv_glue_dbs
    ;;
"hbv_glue" | "ncbi_hbv_glue")
    URL_PREFIX=http://hbv-glue.cvr.gla.ac.uk/hbv_glue_dbs
    ;;
"rabv_glue" | "ncbi_rabv_glue")
    URL_PREFIX=http://rabv.glue.cvr.ac.uk/rabv_glue_dbs
    ;;
"btv_glue")
    URL_PREFIX=http://btv-glue.cvr.gla.ac.uk/btv_glue_dbs
    ;;
"flu_glue")
    URL_PREFIX=http://alpha.cvr.gla.ac.uk/home2/giff01r
    ;;
*)
    echo Unknown GLUE project ${1}
    exit 1
    ;;
esac


export GLUETOOLS_USER=gluetools
export GLUETOOLS_DB=GLUE_TOOLS

echo "Deleting old GLUE database $GLUETOOLS_DB ..."
echo "drop database $GLUETOOLS_DB;" | mysql --user=${GLUETOOLS_USER} --password=glue12345 2> /dev/null
echo "Creating new GLUE database $GLUETOOLS_DB ..."
echo "create database $GLUETOOLS_DB character set UTF8;" | mysql --user=${GLUETOOLS_USER} --password=glue12345

echo "Retrieving $DB_FILE"
 rm -rf ${GLUE_HOME}/tmp/${DB_FILE}
wget -P ${GLUE_HOME}/tmp ${URL_PREFIX}/${DB_FILE}
RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo "Loading MySQL database from file /tmp/${DB_FILE} into GLUE_TOOLS"
    gunzip -c ${GLUE_HOME}/tmp/${DB_FILE} | mysql --user=${GLUETOOLS_USER} --password=glue12345 $GLUETOOLS_DB
    rm ${GLUE_HOME}/tmp/${DB_FILE}
else
    echo Retrieval failed
    exit 1
fi
