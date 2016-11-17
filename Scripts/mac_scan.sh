#!/bin/bash

mac_address=$1

if [[ -n "$mac_address" ]]; then
   nmap -sP 192.168.1.0/24 | grep -B 2 $1 | sed -n '1p' | sed 's/.*(\(.*\))/\1/'
else
   echo "Usage: ./mac_scan.py <MAC_Address>"
fi
