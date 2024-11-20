FROM mysql:5.7.36

ARG glueVersion

# Add the missing public key for MySQL repository
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys B7B3B788A8D3785C

# Update package lists and install wget
RUN apt-get update && \
    apt-get install --no-install-recommends -y wget

# Add necessary scripts and configuration files
ADD docker/init_glue_db.sql /docker-entrypoint-initdb.d
ADD docker/installGlueProject.sh /bin/installGlueProject.sh
ADD docker/glueWipeDatabase.sh /bin/glueWipeDatabase.sh

# Set execute permissions for scripts
RUN chmod u+rwx /bin/installGlueProject.sh
RUN chmod u+rwx /bin/glueWipeDatabase.sh

# Expose MySQL port
EXPOSE 3306

# Set default root password for MySQL
ENV MYSQL_ROOT_PASSWORD=root123
