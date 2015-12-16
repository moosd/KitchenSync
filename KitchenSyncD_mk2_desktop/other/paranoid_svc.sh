#!/bin/bash

pgrep "paranoid_svc.sh" | grep -v $$ && exit 1

journalctl -u NetworkManager -f -n 1 | while read lin; do
    if grep "WiFi hardware radio set enabled" <<<"$lin" > /dev/null; then
       sleep 1
       ifconfig wlan0 down
       /usr/bin/macchanger -a wlan0
       ifconfig wlan0 up
    fi
    if grep "waking up..." <<<"$lin" > /dev/null; then
       sleep 1
       ifconfig wlan0 down
       /usr/bin/macchanger -a wlan0
       ifconfig wlan0 up
    fi
done
