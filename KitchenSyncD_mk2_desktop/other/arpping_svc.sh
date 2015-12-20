#!/bin/bash

pgrep arpping_svc.sh | grep -v $$ && exit 1

# ensure arp reliability whn others connect
echo 1 > /proc/sys/net/ipv4/conf/all/arp_accept

function dumpster(){
    tcpdump -enqtli wlan0 arp | while read r; do echo "$r" | egrep -o "who-has(.+) \\(" && ping -c 1 -W 1.5 $(echo "$r" | egrep -o "who-has(.+) \\(" | cut -d' ' -f2); done
}

function pingitall(){
    fping -g $(ip addr show dev wlan0 | grep "inet " | cut -d' ' -f6) -A -q 
    kitchensend 3 now
}

dumpster&

python /home/souradip/Projects/KitchenSync/KitchenSyncD_mk2_desktop/other/watch_db.py &

# ensure reliability when we connect by spamming unsolicited arp packets

journalctl -u NetworkManager -f | while read lin; do
    if grep ".wlan0.: Activation: successful, device activated." <<<"$lin" > /dev/null; then
        pingitall &
        #arping -q -c 3 -U -I wlan0 $(ip addr show wlan0 | grep "inet "| cut -d' ' -f6 | cut -d'/' -f1)
        dumpster &
    fi
done
