# KitchenSync
Decentralised, encrypted, distributed sync system.

## What is it?
A decentralised sync system for between desktops, laptops and Android phones.

## Why?
I dislike using cloud-based sync services, as I have a very slow internet connection at home. I see no sense in sending data from the UK to America and back again to reach a device centimetres away.

## How?
Each device connects to a network. Upon connecting, it requests an update for any data it is currently tracking. This is done using data encrypted with a shared key, allowing multiple such networks to exist on the same physical network - undecryptable packets are silently dropped. Each device on the network responds to the request by sending a summary of the versions of the files it currently has. The device then works out which device (or itself) has the newest version of the file, and sync this. When a file is updated, a broadcast is sent to the network notifying the connected devices of the update.

Data is not restricted to files and folders; it can be applied to databases (treating each row as a "file" and each table as a "folder").

This therefore supports files to be updated on device A, sync to device B when A and B are connected. A can then disconnect, and the data will be synced to device C when it connects, propagating the updates. In this way, the sync system also doubles as a distributed backup system.

## Where is it?
Under heavy development. Code will trickle onto the repo as it reaches some level of stability.
