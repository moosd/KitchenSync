#!/bin/bash

HOST=`hexdump -n 8 -v -e '/1 "%02x"' -e '/8 "\n"' /dev/urandom`
NAME=android-$HOST

#sed "s/FamilyPC/$NAME/g" < /etc/dhcp/dhclient.conf.temp > /etc/dhcp/dhclient.conf     

touch /tmp/paranoid

systemctl stop sshd
killall -KILL pidgin
killall -KILL avahi-daemon
sleep 2
killall -KILL avahi-daemon
systemctl stop avahi-daemon
systemctl stop avahi-daemon.socket

killall -KILL avahi-daemon
killall -KILL evolution
killall -KILL icedove
killall -KILL java
kill -KILL `ps aux|grep jitsi|grep -v grep|cut -d' ' -f3`
pkill -KILL -u avahi

systemctl start paranoid

if nmcli r wifi|grep enabled; then
nmcli r wifi off
nmcli r wifi on
fi
