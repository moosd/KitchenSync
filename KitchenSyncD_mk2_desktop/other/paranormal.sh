#!/bin/bash

#cp /etc/dhcp/dhclient.conf.norm /etc/dhcp/dhclient.conf

systemctl stop paranoid

ifconfig eth0 down
/usr/bin/macchanger -p eth0
ifconfig eth0 up
sleep 2
ifconfig wlan0 down
/usr/bin/macchanger -p wlan0
ifconfig wlan0 up

#systemctl start sshd
#systemctl start apache2
systemctl start avahi-daemon
systemctl start avahi-daemon.socket

rm /tmp/paranoid

#sudo -u souradip pidgin &
#sudo -u souradip icedove &

