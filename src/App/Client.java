package Zad1;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private JFrame frame;
    private JTextField textField;
    private JTextPane textPane;
    private SocketChannel socketChannel;
    private Queue<String> unreadMessages = new LinkedList<>();
    private Map<String, Integer> topicOffsets = new HashMap<>();

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Client client = new Client();
                client.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Client() {
        initialize();
        connectToServer();
        startReceivingMessages();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        textField = new JTextField();
        panel.add(textField);
        textField.setColumns(10);

        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = textField.getText();
                sendClientMessageToServer(message);
            }
        });
        panel.add(btnSend);

        JButton btnReadMessage = new JButton("Read message");
        btnReadMessage.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        	    if (!unreadMessages.isEmpty()) {
        	        String message = unreadMessages.poll();
        	        String[] messageParts = message.split(":", 2);
        	        String topic = messageParts[0];
        	        String content = messageParts.length > 1 ? messageParts[1] : "";
        	        String fullMessage = topic + ": " + content + "\n";

        	        SimpleAttributeSet greenText = new SimpleAttributeSet();
        	        StyleConstants.setForeground(greenText, Color.GREEN);

        	        try {
        	            int offset = textPane.getDocument().getLength();
        	            textPane.getDocument().insertString(offset, fullMessage, greenText);
        	        } catch (BadLocationException ex) {
        	            ex.printStackTrace();
        	        }

        	        JOptionPane.showMessageDialog(frame, message, "Otrzymana wiadomość", JOptionPane.INFORMATION_MESSAGE);
        	    }
        	}

        });
        panel.add(btnReadMessage);

        JScrollPane scrollPane = new JScrollPane();
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        textPane = new JTextPane();
        textPane.setEditable(false);
        scrollPane.setViewportView(textPane);
    }

    private void connectToServer() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT));
            while (!socketChannel.finishConnect()) {
                System.out.println("Waiting to connect to server...");
            }
            System.out.println("Connected to server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendClientMessageToServer(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(message.getBytes());
        buffer.flip();

        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.clear();
    }

    private void startReceivingMessages() {
        Thread receiveThread = new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            while (true) {
                try {
                    buffer.clear();
                    int bytesRead = socketChannel.read(buffer);

                    if (bytesRead > 0) {
                        buffer.flip();
                        String receivedMessage = new String(buffer.array(), 0, bytesRead);
                        unreadMessages.add(receivedMessage);

                        String topic = receivedMessage.split(":")[0];
                        String notification = "Nowa wiadomość do odczytania na temat: " + topic + "\n";
                        int offset = textPane.getDocument().getLength();

                        try {
                            textPane.getDocument().insertString(offset, notification, null);
                            topicOffsets.put(topic, offset);
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
}