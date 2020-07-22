package io.pravega.example.writers;

import io.pravega.client.stream.impl.UTF8StringSerializer;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;

import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class MqttWriter {
    private static final Logger LOG = LoggerFactory.getLogger(MqttWriter.class);
    private final static URI controllerURI = Parameters.getControllerURI();
    private final static String scope = Parameters.getScope();
    private final static String streamName = Parameters.getStreamName();
    private final static Boolean isEnableTls = Parameters.isEnableTls();
    private final static String tlsTrustStorePath = Parameters.getTrustStorePath();
    private final static Boolean isTlsValidateHostname = Parameters.isValidateHostname();
    private final static String routingKey = Parameters.getRoutingKeyAttributeName();
    private final static String MQTT_BROKER_URL = Parameters.getBrokerUrl();
    private final static String MQTT_TOPIC = Parameters.getTopic();
    private final static String MQTT_CLIENT_ID = Parameters.getClientId();
    private final static Boolean MQTT_isCLEAN_SESSION = Parameters.isClenSession();

    public static void main(String[] args) {
        try {
            LOG.info("START:");
            LOG.info("Connecting to Broker1 using MQTT");
            LOG.info("MQTT_BROKER_URL: " + MQTT_BROKER_URL);
            LOG.info("MQTT_TOPIC: " + MQTT_TOPIC);

            MQTT mqtt = new MQTT();
            mqtt.setHost(MQTT_BROKER_URL);
            mqtt.setClientId(MQTT_CLIENT_ID);
            mqtt.setCleanSession(MQTT_isCLEAN_SESSION);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();

            LOG.info("Connected to MQTT blocker " + MQTT_BROKER_URL);

            // Subscribe to  MQTT topic
            Topic[] topics = {new Topic(MQTT_TOPIC, QoS.AT_MOST_ONCE)};
            connection.subscribe(topics);

            if (Parameters.isPravegaStandalone()) {
                try {Utils.createStream(scope, streamName,controllerURI);}
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ClientConfig clientConfig = ClientConfig.builder().
                    controllerURI(controllerURI)
                    .build();
            if (isEnableTls) {
                clientConfig.toBuilder().trustStore(tlsTrustStorePath).validateHostName(isTlsValidateHostname).build();
            }

            EventStreamClientFactory clientFactory = EventStreamClientFactory.withScope(scope, clientConfig);
            EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName,
                    new UTF8StringSerializer(),
                    EventWriterConfig.builder().build());


            while(true) {
                Message record = connection.receive(1, TimeUnit.SECONDS);
                if (record != null) {
                    record.ack();
                    String message = new String(record.getPayload());
                    LOG.info("Writing message: " + message + " with routing-key: " + routingKey + " to stream " + scope + "/" + streamName);
                    writer.writeEvent(routingKey, message);
                }
            }
        }
        catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
