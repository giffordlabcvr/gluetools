FROM mysql:5.7.24
ARG glueVersion

RUN apt-get update
RUN apt-get install --no-install-recommends -y wget

ADD docker/init_glue_db.sql /docker-entrypoint-initdb.d
ADD docker/installGlueProject.sh /bin/installGlueProject.sh
ADD docker/glueWipeDatabase.sh /bin/glueWipeDatabase.sh

RUN chmod u+rwx /bin/installGlueProject.sh
RUN chmod u+rwx /bin/glueWipeDatabase.sh

EXPOSE 3306

ENV MYSQL_ROOT_PASSWORD=root123

