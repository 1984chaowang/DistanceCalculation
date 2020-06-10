# DistanceCalculator

One simple Flink application to calculate the meaning of the distance changes based on the data from the distance IoT sensor.

Logic:

_Fixed window_

    Calculate the average value of each fixed time window (3s)

_Distance Calculation logical_
    
    Normal: <=5m
    A little Far: > 5m && <= 7m
    Far: > 7m

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