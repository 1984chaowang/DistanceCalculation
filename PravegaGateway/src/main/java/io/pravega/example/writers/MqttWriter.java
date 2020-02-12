package io.pravega.example.writers;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            String BROKER_URL = Parameters.getBrokerUrl();
            String FIDELITY_ADS_TOPIC = Parameters.getTopic();

            System.out.println("Connecting to Broker1 using MQTT");
            System.out.println("BROKER_URL: " + BROKER_URL);
            System.out.println("FIDELITY_ADS_TOPIC: " + FIDELITY_ADS_TOPIC);
            MQTT mqtt = new MQTT();
            mqtt.setHost(BROKER_URL);
            mqtt.setClientId("mqtt001");
            mqtt.setCleanSession(false);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            System.out.println("Connected to Artemis");

            // Subscribe to  fidelityAds topic
            Topic[] topics = {new Topic(FIDELITY_ADS_TOPIC, QoS.EXACTLY_ONCE)};
            connection.subscribe(topics);


            ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
            EventStreamWriter<JsonNode> writer = clientFactory.createEventWriter(streamName,
                    new JsonNodeSerializer(),
                    EventWriterConfig.builder().build());
                while(true) {
                    Message record = connection.receive(5, TimeUnit.SECONDS);
                    if (record != null) {
                        record.ack();
                        String message = new String(record.getPayload());
                        System.out.println("Writing message: " + message + " with routing-key: " + routingKey + " to stream " + scope + "/" + streamName);
                        // Deserialize the JSON message.
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode tree = objectMapper.readTree(message);
                        writer.writeEvent(routingKey, tree);
                    }
                }
            }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
