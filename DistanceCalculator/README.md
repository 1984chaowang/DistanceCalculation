# DistanceCalculator

One simple Flink application to calculate the meaning of the distance changes based on the data from the distance IoT sensor.

Logic:

_Fixed window_

    Calculate the average value of each fixed time window (3s)

_Distance Calculation logical_
    
    Normal: <=1m
    A little Far: > 1m && <= 3m
    Far: > 3m

**options:**

The parameters need to be set:

 **pravega_controller_uri**: 
    
    The URI to the controller in the form "tcp://host:port". default is "tcp://127.0.0.1:9090" if not specified
     
 **pravega_scope**: 
 
    The scope name of the stream to read from. default is "demo" if not specified
 
 **pravega_stream**:
 
    The stream name of the stream to read from. default is "data" if not specified
 
 **influxdb_url**:
 
    The URL of influxdb where Flink sink the result to.  default is "http://127.0.0.1:8086" if not specified