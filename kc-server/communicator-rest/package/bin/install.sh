#!/bin/sh

# Kurento Communicator installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
echo ""
echo "Only root can start Kurento"
echo ""
exit 1
fi

KHC_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create defaults
mkdir -p /etc/default
install -o root -g root -m 644 $KHC_HOME/config/khc.defaults /etc/default/khc

# Add configuration
mkdir -p /etc/kurento/
install -o root -g root $KHC_HOME/config/khc.properties /etc/khc/khc.properties
install -o root -g root $KHC_HOME/config/khc-log4j.properties /etc/khc/khc-log4j.properties

# Install binaries
install -o root -g root -m 755 $KHC_HOME/bin/khc.sh /usr/bin/khc
install -o root -g root -m 755 $KHC_HOME/support-files/khc-init.sh /etc/init.d/khc
mkdir -p /var/lib/khc
install -o root -g root $KHC_HOME/lib/khc.war /var/lib/khc/

# Create directories
mkdir -p /var/log/khc
chown -R nobody /var/log/khc
mkdir -p /var/khc/media
chown -R nobody /var/log/khc

# enable media connector
#update-rc.d khc defaults

# start media connector
#/etc/init.d/khc restart
