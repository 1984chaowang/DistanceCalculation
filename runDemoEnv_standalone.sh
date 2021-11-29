#!/bin/bash

Workdir=`pwd`

echo "###Checking the prerequisite items###"
dockerservice=`service docker status | grep -w running`
if [[ $dockerservice != "" ]];then
	echo "docker service is running!"
else
	echo "docker service is not running, please run 'service docker status' to check the details.";
	exit 1;
fi


if [ -f /usr/local/bin/docker-compose ];then
        echo "docker-compose is installed!"
else
        echo "docker-compose is not installed, please follow the guide in docker.io to install docker-compose.";
        exit 1;
fi

if [ -d "$Workdir" ];then
	echo "Go to working dir at $Workdir"
	cd $Workdir
	pwd
else
  	echo "Demo is not existed, please check if the demo application floder in $Workdir has been removed!";
  	exit 1;
fi


echo "###Fetch the local HOST IP Address###"
HostIP=`ifconfig ens33 | grep -w inet | awk '{print $2}'`
export HOST_IP=$HostIP
echo "HOST_IP is $HostIP"
sed -i "s/url:.*/url: http:\/\/$HOST_IP:8086/g" ./DistanceCalculation/Visualization/Config/influxdb-datasource.yaml


echo "###Strarting Flink Cluster.##"
cd ./flink
docker-compose down
sleep 5s
docker-compose up -d

i=1
while [ $i -le 10 ]
do
	sleep 10s
	if [[ `docker-compose ps --filter State | grep -c Up` == 2 ]];then
        	echo "Flink cluster is running!";
		cd ..;
		break
	fi
        if [[ i = 10 ]];then
		echo "Flink cluster can not be started in 100 seconds!";
	fi
	let i+=1
done	

echo "###Strarting Data Injection/PravegaGW/Pravega/InfluxDB/Grafana.##"
cd ./DistanceCalculation
docker-compose down
sleep 15
docker-compose up -d
while [ $i -le 10 ]
do
        sleep 10s
        if [[ `docker-compose ps --filter State | grep -c Up` == 6 ]];then
                echo "Data Injection/PravegaGW/Pravega/InfluxDB/Grafana are running!";
                cd ..;
                break
        fi
        if [[ i = 10 ]];then
                echo "Data Injection/PravegaGW/Pravega/InfluxDB/Grafana can not be started in 100 seconds!";
        fi
        let i+=1
done


sleep 15

echo "###Submit the Flink Job.##"
cd ./DistanceCalculation
JOBMANAGER_CONTAINER=$(docker ps --filter name=jobmanager --format={{.ID}})
docker cp ./DistanceCalculator/distance-calculator-5.0.jar "$JOBMANAGER_CONTAINER":/distance-calculator-5.0.jar
docker exec "$JOBMANAGER_CONTAINER" flink run /distance-calculator-5.0.jar --pravega_controller_uri tcp://$HOST_IP:9090 --pravega_scope demo --pravega_stream test --influxdb_url http://$HOST_IP:8086

echo "******************************************************************************************************"
echo "*Demo has been successfully loaded.                                                                  *"
echo "*Please go to Grafana to check the results                                                           *"
echo "*Grafana URL:                                                                                        *"
echo "*  http://$HOST_IP:3000/d/7jAI6_-Wk/distance?orgId=1&from=1581820530000&to=1581821760000&refresh=5s  *"
echo "*  username: admin                                                                                   *"
echo "*  password: password                                                                                *"
echo "*                                                                                                    *"
echo "*Please go to Flink to check the delails of job execution                                            *"
echo "*Flink URL:                                                                                          *"
echo "*  http://192.168.17.131:8081/                                                                       *"
echo "******************************************************************************************************"
