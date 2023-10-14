package com.example.chatroomp2pudp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ChatClient extends Application {
    private static final int SERVER_PORT = 12345;
    private static final String SERVER_ADDRESS = "localhost"; // Địa chỉ máy chủ
    private DatagramSocket clientSocket;
    private TextArea chatArea;
    private TextField inputField;
    private String username; // Tên người dùng

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nhập tên người dùng");
        dialog.setHeaderText("Nhập tên người dùng của bạn:");
        dialog.setContentText("Tên:");

        dialog.showAndWait().ifPresent(result -> {
            username = result;
            try {
                clientSocket = new DatagramSocket(); // Khởi tạo clientSocket tại đây
                sendMessage(username + " đã tham gia vào chat room.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn và nhấn Enter để gửi");

        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String message = inputField.getText();
                sendMessage(username + ": " + message);
                inputField.clear();
            }
        });

        VBox vbox = new VBox(chatArea, inputField);
        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client: " + username);
        primaryStage.setOnCloseRequest(e -> {
            sendMessage(username + " đã rời khỏi chat room.");
            System.exit(0);
        });
        primaryStage.show();

        try {
            new Thread(this::receiveMessages).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] sendData = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                clientSocket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                Platform.runLater(() -> chatArea.appendText(message + "\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
