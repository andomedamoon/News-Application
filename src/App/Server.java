package Zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 12345;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private Map<String, Set<SocketChannel>> topicSubscribers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    private void start() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port " + PORT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    acceptClientConnection();
                } else if (key.isReadable()) {
                    handleRequest(key);
                }
            }
        }
    }

    private void acceptClientConnection() throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + client.getRemoteAddress());
    }

    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            System.out.println("Client disconnected");
            return;
        }

        buffer.flip();
        String request = new String(buffer.array(), 0, bytesRead);
        System.out.println("Received request: " + request);

        if (request.startsWith("subscribe")) {
            String topic = request.split(" ")[1];
            topicSubscribers.computeIfAbsent(topic, t -> new HashSet<>()).add(client);
        } else if (request.startsWith("unsubscribe")) {
            String topic = request.split(" ")[1];
            Set<SocketChannel> subscribers = topicSubscribers.get(topic);
            if (subscribers != null) {
                subscribers.remove(client);
            }
        } else if (request.startsWith("create")) {
            String topic = request.split(" ")[1];
            topicSubscribers.putIfAbsent(topic, new HashSet<>());
        } else if (request.startsWith("delete")) {
            String topic = request.split(" ")[1];
            topicSubscribers.remove(topic);
        } else if (request.startsWith("message")) {
            String[] parts = request.split(" ", 3);
            String topic = parts[1];
            String message = parts[2];
            Set<SocketChannel> subscribers = topicSubscribers.get(topic);
            if (subscribers != null) {
                for (SocketChannel subscriber : subscribers) {
                    ByteBuffer msgBuffer = ByteBuffer.allocate(256);
                    msgBuffer.put(("Wiadomość na temat " + topic + ": " + message).getBytes());
                    msgBuffer.flip();
                    subscriber.write(msgBuffer);
                    msgBuffer.clear();
                }
            }
        }
    }
}
