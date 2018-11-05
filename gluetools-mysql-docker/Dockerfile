FROM mysql:5.7.24
ARG glueVersion

ADD docker/init_glue_db.sql /docker-entrypoint-initdb.d

EXPOSE 3306

ENV MYSQL_ROOT_PASSWORD=root123

