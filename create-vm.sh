#!/usr/bin/env bash

rootname=k-raider
orga=the-plan

for i in `seq 1 15`;
do
  echo "creating $rootname$i application..."
  clever create -t maven $rootname$i --org $orga --region par --alias $rootname$i --github the-plan/k-raider;
  clever service link-addon $addon --alias $rootname$i;
  clever env set PORT 8080 --alias $rootname$i;
  clever env set SERVICE_PORT 80 --alias $rootname$i;
  clever env set SERVICE_NAME $rootname$i --alias $rootname$i;
  clever env set SERVICE_HOST $rootname$i.cleverapps.io --alias $rootname$i;
  clever env set REDIS_CHANNEL the-plan --alias $rootname$i;
  clever env set REDIS_RECORDS_KEY the-plan-ms --alias $rootname$i;
  clever env set REDIS_HOST	bemc5dzlk-redis.services.clever-cloud.com --alias $rootname$i;
  clever env set REDIS_PASSWORD	iowq14uIA6QdhaszHTq --alias $rootname$i;
  clever env set REDIS_PORT	3096 --alias $rootname$i;
  clever env set REDIS_URL	redis://:iowq14uIA6QdhaszHTq@bemc5dzlk-redis.services.clever-cloud.com:3096 --alias $rootname$i;
  clever domain add $rootname$i.cleverapps.io --alias $rootname$i;
  clever scale --flavor S --alias $rootname$i;
  clever restart --quiet true --alias $rootname$i;
done

