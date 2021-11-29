#!/bin/bash

Workdir=`pwd`
echo "###Stop the Flink Job.##"
cd ./DistanceCalculation
kubectl delete -f FlinkJob_SensorMonitor.yml

sleep 15

echo "###Stopping the data injection###"
ps aux | grep rand | awk  '{print $2}' | xargs -i kill -9 {}

sleep 15

echo "###Stopping PravegaGW/Pravega/InfluxDB/Grafana.##"
cd ./DistanceCalculation
docker-compose down


echo "******************************************************************************************************";
echo "*Demo has been successfully stopped.                                                                 *";
echo "******************************************************************************************************";