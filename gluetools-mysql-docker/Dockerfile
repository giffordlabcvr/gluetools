FROM mysql:5.7.36

ARG glueVersion

# Add the missing public key for MySQL repository
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys B7B3B788A8D3785C

# Fix archived Debian Buster repos and make apt tolerant of the archive mirror
RUN set -eux; \
  sed -i 's|http://deb.debian.org/debian|http://archive.debian.org/debian|g' /etc/apt/sources.list; \
  sed -i 's|http://security.debian.org/debian-security|http://archive.debian.org/debian-security|g' /etc/apt/sources.list; \
  printf 'Acquire::Check-Valid-Until "false";\nAcquire::By-Hash "no";\n' > /etc/apt/apt.conf.d/99archive; \
  apt-get update; \
  apt-get install --no-install-recommends -y wget; \
  rm -rf /var/lib/apt/lists/*

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
