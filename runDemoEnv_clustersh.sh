#!/bin/bash

Workdir=`pwd`

echo "###Checking the prerequisite items###"
dockerservice=`service docker status | grep -w running`
if [[ $dockerservice != "" ]];then
        echo "docker service is running!"
else
        echo "docker service is not running, please run 'service docker status' to check the details."
        exit 1;
fi


if [ -f /usr/local/bin/docker-compose ];then
        echo "docker-compose is installed!"
else
        echo "docker-compose is not installed, please follow the guide in docker.io to install docker-compose."
        exit 1;
fi

if [ -d "$Workdir" ];then
        echo "Go to working dir at $Workdir"
        cd $Workdir
        pwd
else
        echo "Demo is not existed, please check if the demo application floder in $Workdir has been removed!"
        exit 1;
fi


echo "###Fetch the local HOST IP Address###"
HostIP=`ifconfig ens160 | grep -w inet | awk '{print $2}'`
export HOST_IP=$HostIP
echo "HOST_IP is $HostIP"
sed -i "s/url:.*/url: http:\/\/$HOST_IP:8086/g" ./DistanceCalculation/Visualization/Config/influxdb-datasource.yaml


echo "###Starting PravegaGW/Pravega/InfluxDB/Grafana.##"
cd $Workdir/DistanceCalculation
docker-compose down >/dev/null 2>&1
sleep 15
docker-compose up -d
i=0
while [ $i -le 10 ]
do
        sleep 10s
        if [[ `docker-compose ps --filter State | grep -c Up` == 4 ]];then
                echo "PravegaGW/Pravega/InfluxDB/Grafana are running!"
                cd ..
                break
        fi
        if [[ i = 10 ]];then
                echo "PravegaGW/Pravega/InfluxDB/Grafana can not be started in 100 seconds!"
        fi
        let i++
done


echo "###Deploying Flink Cluster###"
cd $Workdir/DistanceCalculation
kubectl delete -f FlinkCluster.yml >/dev/null 2>&1
kubectl apply -f FlinkCluster.yml &
i=0
while [ $i -le 10 ]
do
        FLINKAPP_STATE=$(kubectl get FlinkCluster flink1 -o jsonpath='{.status.state}' -n demo)
        sleep 10s
        if [[ $FLINKAPP_STATE = Running ]];then
             echo "******************************************************************************************************"
             echo "*FlinlCluster flink1 has been successfully deployed.                                                 *"
             echo "******************************************************************************************************"
             break;
        fi
        if [[ i -eq 10 ]];then
             echo "FlinkApplication failed to start!"
             exit 1
        fi
        let i++
done


echo "###Starting Data Injection##"
cd $Workdir/DistanceCalculation/randsend_simple_new/src
./randSend.lxe >/dev/null 2>&1 &
echo "Data Injection is running!"
cd $Workdir
sleep 5


echo "###Submitting the Flink Job.###"
cd ./DistanceCalculation
kubectl delete -f FlinkJob_SensorMonitor >/dev/null 2>&1
kubectl apply -f FlinkJob_SensorMonitor.yml &
i=0
while [ $i -le 20 ]
do
        FLINKAPP_STATE=$(kubectl get FlinkApplication sensor-monitor-job -o jsonpath='{.status.state}' -n demo)
        sleep 10s
        if [[ $FLINKAPP_STATE = started ]];then
             echo "******************************************************************************************************"
             echo "*Demo has been successfully loaded.                                                                  *"
             echo "*Please go to Grafana to check the results                                                           *"
             echo "*Grafana URL:                                                                                        *"
             echo "*  http://$HOST_IP:3000/d/feLYIRGMz/sensors?orgId=1&refresh=10s                                      *"
             echo "*  username: admin                                                                                   *"
             echo "*  password: password                                                                                *"
             echo "*                                                                                                    *"
             echo "*Please go to Flink to check the delails of job execution                                            *"
             echo "*Flink URL:                                                                                          *"
             echo "*  http://192.168.17.131:8081/                                                                       *"
             echo "*                                                                                                    *"
             echo "* Please do not forget to stop the test by executing the script /"stopDemoEnv.sh/"                   *"
             echo "******************************************************************************************************"
             break
        fi
        if [[ i -eq 20 ]];then
             echo "FlinkApplication failed to start!"
             exit 1
        fi
        let i++
done