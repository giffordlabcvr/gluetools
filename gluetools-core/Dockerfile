FROM ubuntu:16.04
ARG glueVersion

RUN apt-get update && apt-get install -y apt-transport-https
# debconf-utils and python-software-properties manipulate package installer settings to allow non-interactive config
# software-properties-common adds the add-apt-repository command.
RUN apt-get install --no-install-recommends -y debconf-utils \
	python-software-properties \
	software-properties-common	    
	    
# OpenJDK
RUN apt-get install --no-install-recommends -y default-jre

# various stuff required for MAFFT, RAxML 
RUN apt-get install --no-install-recommends -y git
RUN apt-get install --no-install-recommends -y make 
RUN apt-get install --no-install-recommends -y gcc
RUN apt-get install --no-install-recommends -y libc6-dev
RUN apt-get install --no-install-recommends -y unzip

RUN mkdir -p /builds/

# RAxML
RUN cd /builds; git clone https://github.com/stamatak/standard-RAxML.git
RUN cd /builds/standard-RAxML/ ; git checkout 3abe69b
RUN cd /builds/standard-RAxML/ ; make -f Makefile.SSE3.PTHREADS.gcc
RUN mkdir -p /opt/gluetools/raxml/bin
RUN mkdir -p /opt/gluetools/tmp/raxmlfiles
RUN cp /builds/standard-RAxML/raxmlHPC-PTHREADS-SSE3 /opt/gluetools/raxml/bin

# mafft
COPY docker/mafft-7.407-with-extensions-src.tgz /builds
RUN mkdir -p /opt/gluetools/tmp/mafftfiles
RUN cd /builds ; tar xvfz mafft-7.407-with-extensions-src.tgz
RUN cd /builds/mafft-7.407-with-extensions/core ; make clean ; make ; make install

# BLAST+
RUN mkdir -p /opt/gluetools/blast/ncbi-blast-2.2.31+
COPY docker/ncbi-blast-2.2.31+ /opt/gluetools/blast/ncbi-blast-2.2.31+
RUN chmod a+rwx /opt/gluetools/blast/ncbi-blast-2.2.31+/bin/*
RUN mkdir -p /opt/gluetools/blast/ncbi-blast-2.2.31+/blastdbs
RUN mkdir -p /opt/gluetools/tmp/blastdbs
RUN mkdir -p /opt/gluetools/tmp/blastfiles

# tbl2asn
RUN mkdir -p /opt/gluetools/tbl2asn/bin
RUN mkdir -p /opt/gluetools/tmp/tbl2asn
COPY docker/tbl2asn /opt/gluetools/tbl2asn/bin
RUN chmod a+rwx /opt/gluetools/tbl2asn/bin/*

# clusterPicker
RUN mkdir -p /opt/gluetools/tmp/clusterPicker
RUN mkdir -p /opt/gluetools/clusterPicker/lib
COPY docker/ClusterPicker_1.2.5.jar /opt/gluetools/clusterPicker/lib

# jModelTest
RUN mkdir -p /opt/gluetools/tmp/jmodeltest
RUN mkdir -p /opt/gluetools/jModelTest/lib
COPY docker/jModelTest.jar /opt/gluetools/jModelTest/lib

RUN mkdir -p /opt/gluetools/lib
RUN mkdir -p /opt/gluetools/conf
RUN mkdir -p /opt/gluetools/projects
RUN mkdir -p /opt/gluetools/tmp/sam

COPY docker/exampleProject-${glueVersion}.zip /opt/gluetools/projects
RUN cd /opt/gluetools/projects && unzip exampleProject-${glueVersion}.zip && rm exampleProject-${glueVersion}.zip

COPY docker/gluetools-core-${glueVersion}.jar /opt/gluetools/lib/gluetools-core.jar
COPY docker/gluetools.sh /opt/gluetools/bin/gluetools.sh
RUN chmod a+rwx /opt/gluetools/bin/gluetools.sh
COPY docker/gluetools-config.xml /opt/gluetools/conf
COPY docker/.gluerc /root


ENV GLUE_HOME=/opt/gluetools
ENV PATH="/opt/gluetools/bin:${PATH}"
ENV LANG=C.UTF-8

# Set the working directory to /opt/gluetools/projects/exampleProject
WORKDIR /opt/gluetools/projects/exampleProject

CMD gluetools.sh

