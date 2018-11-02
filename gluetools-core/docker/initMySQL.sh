#!/bin/sh
/opt/gluetools/bin/startMySQL.sh
echo 'GRANT ALL ON GLUE_TOOLS.* TO gluetools@localhost identified by "glue12345";' | mysql -u root --password=root123
echo 'create database GLUE_TOOLS character set UTF8;' | mysql -u gluetools --password=glue12345 
