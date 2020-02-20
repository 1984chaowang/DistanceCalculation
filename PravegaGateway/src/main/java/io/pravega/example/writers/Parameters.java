package io.pravega.example.writers;

import java.net.URI;

// All parameters will come from environment variables. This makes it easy
// to configure on Docker, Mesos, Kubernetes, etc.
public class Parameters {
    // By default, we will connect to a standalone Pravega running on localhost.
    public static URI getControllerURI() {
        return URI.create(getEnvVar("PRAVEGA_CONTROLLER", "tcp://localhost:9090"));
    }
    public static String getScope() {
        return getEnvVar("PRAVEGA_SCOPE", "daduriver-demo");
    }
    public static String getStreamName() {
        return getEnvVar("PRAVEGA_STREAM", "daduriver-data");
    }
    public static int getScaleFactor() {
        return Integer.parseInt(getEnvVar("PRAVEGA_SCALE_FACTOR", "2"));
    }
    public static int getMinNumSegments() {
        return Integer.parseInt(getEnvVar("PRAVEGA_MIN_NUM_SEGMENTS", "1"));
    }
    public static String getRoutingKeyAttributeName() {
        return getEnvVar("ROUTING_KEY_ATTRIBUTE_NAME", "remote_addr");
    }
    public static  String getBrokerUrl() {
        return getEnvVar("MQTT_BROKER_URL", "tcp://10.37.1.207:1883");
    }
    public static  String getTopic() {
        return getEnvVar("MQTT_TOPIC", "FIDELITY.ADS");
    }

    private static String getEnvVar(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public static boolean isPravegaStandalone() {
        return getEnvVar("PRAVEGA_STANDALONE", "false").equals("true");
    }
}
