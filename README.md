I made a light Pravega/Fink demo including the data injection/analytics/visualization, it could be run in an Ubuntu VM and all components are able to be deployed by docker-compose.
 
Basically, the demo uses the pre-recorded distance data from a sensor, it simulates them be written into a Pravega stream and then be processed by a simple logic implement in Flink job, the computing result are countinusely sink to a influxdb which is presented on Grafana dashboard.
