package Zad1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Admin {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private JFrame frame;
    private JTextField textField;
    private SocketChannel socketChannel;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Admin admin = new Admin();
                admin.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Admin() {
        initialize();
        connectToServer();
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
                sendAdminMessageToServer(message);
            }
        });
        
        panel.add(btnSend);
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

    private void sendAdminMessageToServer(String message) {
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
}