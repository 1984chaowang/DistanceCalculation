# PravegaGateway
Data Ingestion into pravega through socket as gateway.

Message should be sent to socket line by line and string "FINISH" will be deemed as signal of ending the communication and will close the gateway

options:

-s "scope": The scope name of the stream to read from. default is "demo" if not specified
-n "name": The name of the stream to read from. default is "demoStream" if not specified
-u "uri": The URI to the controller in the form "tcp://host:port". default is "tcp://127.0.0.1:9090" if not specified
-r "routingKey": The routing key of the message to write. default is "demoRoutingKey" if not specified

