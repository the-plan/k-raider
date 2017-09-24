#!/usr/bin/env bash

rootname=k-raider

for i in `seq 4 7`;
do
  echo "deleting $rootname$i application..."
  clever delete --alias $rootname$i --yes
done