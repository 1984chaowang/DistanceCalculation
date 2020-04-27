package io.pravega.example.writers;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import io.pravega.client.ClientFactory;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;

import java.net.URI;

public class MqttWriter {

    public static void main(String[] args) {
        try {
            System.out.println("START:");
            URI controllerURI = Parameters.getControllerURI();
            String scope = Parameters.getScope();
            String streamName = Parameters.getStreamName();
            String routingKey = Parameters.getRoutingKeyAttributeName();
            String MQTT_BROKER_URL = Parameters.getBrokerUrl();
            String MQTT_TOPIC = Parameters.getTopic();

            System.out.println("Connecting to Broker1 using MQTT");
            System.out.println("MQTT_BROKER_URL: " + MQTT_BROKER_URL);
            System.out.println("MQTT_TOPIC: " + MQTT_TOPIC);
            MQTT mqtt = new MQTT();
            mqtt.setHost(MQTT_BROKER_URL);
            mqtt.setClientId("mqtt001");
            mqtt.setCleanSession(false);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            System.out.println("Connected to MQTT blocker " + MQTT_BROKER_URL);

            // Subscribe to  MQTT topic
            Topic[] topics = {new Topic(MQTT_TOPIC, QoS.AT_LEAST_ONCE)};
            connection.subscribe(topics);

            if (Parameters.isPravegaStandalone()) {
                try {Utils.createStream(scope, streamName,controllerURI);}
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
            EventStreamWriter<JsonNode> writer = clientFactory.createEventWriter(streamName,
                    new JsonNodeSerializer(),
                    EventWriterConfig.builder().build());

            while(true) {
                Message record = connection.receive(1, TimeUnit.SECONDS);
                if (record != null) {
                    record.ack();
                    String message = new String(record.getPayload());

                    // Deserialize the JSON message.
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode element = objectMapper.readTree(message);
                    System.out.println("Writing message: " + element.toString() + " with routing-key: " + routingKey + " to stream " + scope + "/" + streamName);
                    writer.writeEvent(routingKey, element);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
