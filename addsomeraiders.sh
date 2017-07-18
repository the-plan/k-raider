#!/usr/bin/env bash


startPort=9090

for i in `seq 16 25`;
do
    resultat=$(($startPort+$i))
    REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="R" SERVICE_PORT=$resultat PORT=$resultat java  -jar target/k-raider-1.0-SNAPSHOT-fat.jar&
done

