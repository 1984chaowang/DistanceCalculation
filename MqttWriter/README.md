# MQTTWriter
A toolkit as a simulator to write the data into MQTT and later be consumed by the GW which reads the data into Pravega, it keeps running until you stop this toolkit.


You can either deploy it via docker or run in your IDE, some env variables shall be stated.


**options:**

_MQTT_BROKER_URL_, the MQTT broker url, the default setting is "tcp://127.0.0.1:1883";

_MQTT_TOPIC_, the MQTT topic where you are going to read the data from, the default setting is "test";

_MQTT_DATA_FILE_, the data you are going to inject into MQTT, the default setting i "Distance.csv".


