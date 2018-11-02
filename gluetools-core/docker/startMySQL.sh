#!/bin/sh
# workaround for this issue:
# https://serverfault.com/questions/870568/fatal-error-cant-open-and-lock-privilege-tables-table-storage-engine-for-use
mkdir -p /var/run/mysqld
chown -R mysql:mysql /var/lib/mysql /var/run/mysqld

# start mysql
service mysql start