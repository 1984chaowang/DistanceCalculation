# PravegaGateway
Data Ingestion into pravega through socket as gateway.

It is going to emulate the typical IoT scenario by using MQTT to connect all IoT sensors.
Messages should be sent to MQTT, GW consumes the messages from MQTT and meanwhile writes into Pravega.

**options:**

The env variables need to be set:

 **PRAVEGA_CONTROLLER**: 
 
       The URI to the controller in the form "tcp://host:port". default is "tcp://127.0.0.1:9090" if not specified
 
 **PRAVEGA_SCOPE**: 
 
        The scope name of the stream to write into. default is "demo" if not specified
 
 **PRAVEGA_STREAM**: 
 
        The stream name of the stream to write into. default is "data" if not specified
 
 **PRAVEGA_STANDALONE**: 
 
        The indicator of Pravega crentials enabled. default is "false" if not specified
 
 **ROUTING_KEY_ATTRIBUTE_NAME**:
 
        The routing key of the message to write. default is "remote_addr" if not specified
 
 **MQTT_BROKER_URL**: 
 
    The MQTT broker URL where the GW connects to. default is "tcp://127.0.0.1:1883" if not specified
 
 **MQTT_TOPIC**: 
 
     The topic name where the GW consumes from. default is "demo" if not specified
     
 
 The env variables for Pravega Credential:
 
         NAME	VALUE
         pravega_client_auth_method	Bearer
         pravega_client_auth_loadDynamic	true
         KEYCLOAK_SERVICE_ACCOUNT_FILE	Path to Keycloak OIDC JSON client configuration file (keycloak.json).
         
         Configure Nautilus Authentication
         
         Create a project workshop-samples in Nautilus UI
         This will automatically create a scope workshop-samples
         Get the keycloak.json file by executing this command
         
         kubectl get secret workshop-samples-pravega -n workshop-samples 
         -o jsonpath="{.data.keycloak\.json}" |base64 -d >  ${HOME}/keycloak.json
         chmod go-rw ${HOME}/keycloak.json
         
         output looks like the following:
         
         {
           "realm": "nautilus",
           "auth-server-url": "https://keycloak.p-test.nautilus-lab-wachusett.com/auth",
           "ssl-required": "external",
           "bearer-only": false,
           "public-client": false,
           "resource": "workshop-samples-pravega",
           "confidential-port": 0,
           "credentials": {
             "secret": "c72c45f8-76b0-4ca2-99cf-1f1a03704c4f"
           }
         }

