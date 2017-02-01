#!/bin/bash -x

TOMCAT_PATH="/opt/apache-tomcat-7.0.75"
TOMCAT_TIMEOUT=180
KHC_PATH="/etc/khc"

RESOURCE_PATH="/root/docker-res-files"

RABBITMQ_TIMEOUT=180
RABBITMQ_PATH="/var/log/rabbitmq"

export JAVA_HOME="/usr/lib/jvm/java-8-oracle/jre"

rabbitmq-server -detached

j=1
while [ $j -le $RABBITMQ_TIMEOUT ]; do
    grep -q "Server startup complete" $RABBITMQ_PATH/rabbit@*.log && break
    sleep 1
    [ $j -eq $RABBITMQ_TIMEOUT ] && {
        echo "ERROR 23: Could not start Rabbitmq server";
        return 23;
    }
    j=$(( $j + 1 ))
done

echo "Rabbitmq started!";

sudo service mysql restart

sudo service apache2 restart

cd $TOMCAT_PATH/bin
./startup.sh

i=1
while [ $i -le $TOMCAT_TIMEOUT ]; do
    grep -q "Server startup in" $TOMCAT_PATH/logs/catalina.out && break
    sleep 1
    [ $i -eq $TOMCAT_TIMEOUT ] && {
        echo "ERROR 23: Could not start tomcat Server";
        return 23;
    }
    i=$(( $i + 1 ))
done

echo "Tomcat started!";

mysql -u khc -pkhc kagenda <$RESOURCE_PATH/createdb.sql

cd $KHC_PATH
sed -i 's/hibernate.hbm2ddl.auto=update/hibernate.hbm2ddl.auto=validate/g' khc.properties

tail -f /dev/null
