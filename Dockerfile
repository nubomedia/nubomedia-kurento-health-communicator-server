FROM ubuntu:14.04

MAINTAINER Naevatec

ADD . /root

RUN sudo apt-get update -y > /dev/null
RUN sudo apt-get upgrade -y > /dev/null

# ---VIM---
RUN sudo apt-get install vim -y > /dev/null

# --- COMPILE WITH MAVEN ---  
# ---Java---
RUN sudo apt-get install -y software-properties-common > /dev/null
RUN sudo add-apt-repository ppa:webupd8team/java -y > /dev/null
RUN sudo apt-get update > /dev/null
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections > /dev/null
RUN sudo apt-get install -y oracle-java8-installer > /dev/null
ENV JAVA_HOME=/usr/lib/jvm/java-8-oracle/jre

# ---Maven---
RUN sudo apt-get install maven -y > /dev/null

# ---INSTALL GCM JAR IN LOCAL REPOSITORY---
RUN mvn install:install-file -Dfile=/root/docker-res-files/gcm-server-4.jar -DpomFile=/root/docker-res-files/pom.xml

WORKDIR /root
RUN mvn clean install -DskipTests
WORKDIR /
# --------------------------

# ---WGET---
RUN sudo apt-get update > /dev/null
RUN sudo apt-get install wget -y > /dev/null

# ---UNZIP---
RUN sudo apt-get install unzip -y > /dev/null

# ---RABBIT MQ---
RUN sudo echo 'deb http://www.rabbitmq.com/debian/ testing main' | sudo tee /etc/apt/sources.list.d/rabbitmq.list
RUN wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -
RUN sudo apt-get update > /dev/null
RUN sudo apt-get install rabbitmq-server -y > /dev/null

# ---MySQL---
# Server installation
RUN sudo apt-get install mysql-server -y > /dev/null
# It is necessary to create a database named 'kagenda' and a new user 'khc' with all priviledges over this database.
RUN cp /root/docker-res-files/init_db.sh /tmp/init_db.sh
RUN sudo chmod +x /tmp/init_db.sh
RUN sudo /tmp/init_db.sh

# ---Tomcat 7.0.76--- 
# Download the server distribution file and extract it over the desired directory.
WORKDIR /opt
RUN wget http://apache.uvigo.es/tomcat/tomcat-7/v7.0.76/bin/apache-tomcat-7.0.76.zip > /dev/null
RUN unzip apache-tomcat-7.0.76.zip > /dev/null
WORKDIR /

# ---Apache2---
# Server installation
RUN sudo apt-get install apache2 -y > /dev/null

# Configure '/etc/apache2/sites-available/000-default.conf' and add it to sites-enabled:
RUN sudo cp /root/docker-res-files/khc_enabled.conf /etc/apache2/sites-available/
RUN sudo a2ensite khc_enabled

# Create '/etc/apache2/ssl' with the certificates for being used by apache2.
RUN sudo mkdir /etc/apache2/ssl
RUN sudo cp /root/docker-res-files/nubomedia_khc_server.crt /etc/apache2/ssl/
RUN sudo cp /root/docker-res-files/nubomedia_khc_server.key /etc/apache2/ssl/

# Install libapache2-mod-jk:
RUN sudo apt-get install libapache2-mod-jk -y
# Configure the 'workers.properties' file inside '/etc/libapache2-mod-jk/'
RUN sudo rm /etc/libapache2-mod-jk/workers.properties
RUN sudo cp /root/docker-res-files/workers.properties /etc/libapache2-mod-jk/

# Install libapache2-mod-proxy-html
RUN sudo apt-get install libapache2-mod-proxy-html
# RUN sudo a2enmod mod_proxy
RUN sudo a2enmod proxy

# Add ssl module to Apache2
RUN sudo a2enmod ssl

# Add proxy websocket tunnel module to Apache2
RUN sudo a2enmod proxy_wstunnel

# Finally, restart Apache2 service
RUN sudo service apache2 restart

# ---KC Server---
# Copy the kc.war file to Tomcat´s webapps directory.
RUN sudo cp /root/kc-server/communicator-rest/target/communicator-rest-2.4.3-SNAPSHOT.war /opt/apache-tomcat-7.0.76/webapps/khcrest.war

# Create '/etc/khc' directory.
RUN sudo mkdir /etc/khc
# Copy the properties file 'khc.properties' to '/etc/khc' directory.
RUN sudo cp /root/docker-res-files/khc.properties /etc/khc/khc.properties

# Create '/var/khc' directory.
RUN sudo mkdir /var/khc
# Create '/var/khc/media' directory.
RUN sudo mkdir /var/khc/media
# Create '/var/log/khc' directory.
RUN sudo mkdir /var/log/khc
# Modify '/etc/khc/khc.properties' file as needed.
# Modify in khc.properties 'hibernate.hbm2ddl.auto' property to 'update' for generate the database scheme (after the scheme has been created, set this property to # 'validate').
# Take a special care with the GCM key set in 'kurento.gcm.key' property.

WORKDIR /opt/apache-tomcat-7.0.76
RUN sudo chmod +x -R bin/

WORKDIR /

RUN sudo chmod +x /root/docker-res-files/entry_point.sh
ENTRYPOINT ["/root/docker-res-files/entry_point.sh"]
