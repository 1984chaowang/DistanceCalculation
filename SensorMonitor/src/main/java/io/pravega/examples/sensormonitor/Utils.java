package io.pravega.examples.sensormonitor;

import io.pravega.client.ClientConfig;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamConfiguration;

import java.net.URI;

public class Utils {
    /**
     * Creates a Pravega stream with a given configuration.
     *
     * @param scope the Pravega configuration.
     * @param streamName the stream name (qualified or unqualified).
     * @param controllerURI the stream configuration (scaling policy, retention policy).
     */
    public static boolean  createStream(String  scope, String streamName, URI controllerURI) {
        boolean result = false;
        // Create client config
        ClientConfig clientConfig = ClientConfig.builder().controllerURI(controllerURI).build();
        //System.out.println(clientConfig);
	try(StreamManager streamManager = StreamManager.create(clientConfig);)
        {
           //System.out.println("CREATE STREAM MANAGER");
	    if (Parameters.isPravegaStandalone()) {
                streamManager.createScope(scope);
                System.out.println("DONE: " + scope + "has been created");
            }
            result = streamManager.createStream(scope, streamName, StreamConfiguration.builder().build());
	        System.out.println("DONE: " + scope +"/" + streamName + "has been created");
        }
        catch (Exception e) {
            e.printStackTrace();
	    throw new RuntimeException(e);
        }
	System.out.println(result);
        if (result) {
          // System.out.println("CREATE STREAM MANAGER SUCCESSFULLY");
	} else { 
	   //System.out.println("CREATE STREAM MANAGER FAILED");
	}
	return result;
    }
}
