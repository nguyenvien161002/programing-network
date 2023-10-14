package com.example.chatroomp2pudp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ChatServer extends Application {
    private static final int PORT = 12345;
    private static List<ClientInfo> clients = new ArrayList<>();
    private static TextArea serverTextArea;
    private static ListView<String> clientListView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        serverTextArea = new TextArea();
        serverTextArea.setEditable(false);
        serverTextArea.setWrapText(true);

        clientListView = new ListView<>();

        VBox vbox = new VBox(serverTextArea, clientListView);
        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Server");
        primaryStage.setOnCloseRequest(e -> {
            sendMessageToAllClients("Máy chủ đã tắt.");
            System.exit(0);
        });
        primaryStage.show();

        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            Platform.runLater(() -> serverTextArea.appendText("Máy chủ chat đang chạy...\n"));

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                Platform.runLater(() -> serverTextArea.appendText("Nhận từ " + receivePacket.getAddress() + ": " + message + "\n"));

                // Xử lý thông điệp ở đây và gửi lại các tin nhắn đến các chatters khác (P2P lai)
                sendMessageToAllClients(message);

                // Thêm thông tin người dùng vào danh sách nếu chưa tồn tại
                boolean isNewUser = true;
                for (ClientInfo client : clients) {
                    if (client.getAddress().equals(receivePacket.getAddress()) && client.getPort() == receivePacket.getPort()) {
                        isNewUser = false;
                        break;
                    }
                }
                if (isNewUser) {
                    clients.add(new ClientInfo(receivePacket.getAddress(), receivePacket.getPort()));
                    System.out.println("receivePacket.getAddress(): " + receivePacket.getAddress());
                    System.out.println("receivePacket.getPort(): " + receivePacket.getPort());
                    Platform.runLater(() -> updateClientListView());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToAllClients(String message) {
        for (ClientInfo client : clients) {
            sendMessageToClient(message, client.getAddress(), client.getPort());
        }
    }

    private void sendMessageToClient(String message, InetAddress address, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateClientListView() {
        clientListView.getItems().clear();
        for (ClientInfo client : clients) {
            clientListView.getItems().add(client.getAddress() + ":" + client.getPort());
        }
    }

    private static class ClientInfo {
        private final InetAddress address;
        private final int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }
    }
}

