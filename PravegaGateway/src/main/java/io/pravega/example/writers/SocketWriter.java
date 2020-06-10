package io.pravega.example.writers;

//import com.configuration.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pravega.client.ClientConfig;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

public class SocketWriter {
    private final Socket socket;
    private final String scope;
    private final String streamName;
    private final URI controllerURI;

    public SocketWriter(Socket socket, String scope, String streamName, URI controllerURI) {
        this.socket = socket;
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = controllerURI;
    }

    // listen to a socket
    public void run(String routingKey, ClientConfig clientConfig) throws IOException {
        String message;
        try (
                EventStreamClientFactory clientFactory = EventStreamClientFactory.withScope(scope, clientConfig);
                EventStreamWriter<JsonNode> writer = clientFactory.createEventWriter(streamName,
                        new JsonNodeSerializer(),
                        EventWriterConfig.builder().build());
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while (true) {
                //System.out.println("START Reading:");
                message = br.readLine();
                if (message == null) continue;
                if (message.equals("FINISH")) break;
                System.out.println(message);
                // Deserialize the JSON message.
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode tree = objectMapper.readTree(message);
                writer.writeEvent(routingKey, tree);
            }
        } 
     	catch (Exception e) {
            e.printStackTrace();
        }   
    }

    public static void main(String[] args) {

        try {
	    System.out.println("START:");
            URI controllerURI = Parameters.getControllerURI();
            String scope = Parameters.getScope();
            String streamName = Parameters.getStreamName();
            String routingKey = Parameters.getRoutingKeyAttributeName();

            // create a socket server
            final ServerSocket ss = new ServerSocket(9999);
            // wait and listen to the data input client.
            Socket socket = ss.accept();
            SocketWriter writer = new SocketWriter(socket, scope, streamName, controllerURI);
            ClientConfig clientConfig = ClientConfig.builder().controllerURI(controllerURI).build();

            writer.run(routingKey, clientConfig);

            socket.close();
            ss.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

