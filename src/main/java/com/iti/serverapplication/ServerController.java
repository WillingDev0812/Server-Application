package com.iti.serverapplication;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class ServerController implements Initializable {

    @FXML
    private TextField portField;

    @FXML
    private Button startServerBtn;

    @FXML
    private Button stopServerBtn;

    @FXML
    private Label statusText;

    private ServerSocket serverSocket;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stopServerBtn.setVisible(false);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
    }

    public void startServer(ActionEvent ae) {
        if (portField.getText().isEmpty() || !isValidPort(portField.getText())) {
            showAlert(Alert.AlertType.ERROR, "Server Failed to start", "The port number is not valid");
            return;
        }

        int port = Integer.parseInt(portField.getText());

        try {
            DatabaseConnectionManager.connect();
            serverSocket = new ServerSocket(port);
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        new Thread(new ServerClientHandler(serverSocket.accept())).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            stopServerBtn.setVisible(true);
            startServerBtn.setVisible(false);
            statusText.setTextFill(Color.GREEN);
            statusText.setText("Online");
            portField.setDisable(true);
            showAlert(Alert.AlertType.CONFIRMATION, "Server Started", "Server started successfully");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Server Failed to start", "Database connection failed");
            e.printStackTrace();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Server Failed to start", "Failed to start server socket");
            e.printStackTrace();
        }
    }

    private void setServerStopped() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            DatabaseConnectionManager.disconnect();
            showAlert(Alert.AlertType.CONFIRMATION, "Server Stopped", "Server stopped");

        } catch (SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Failed to stop server", "Error occurred while stopping the server");
            e.printStackTrace();
        }

        stopServerBtn.setVisible(false);
        startServerBtn.setVisible(true);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
        portField.setDisable(false);
    }


    private boolean isValidPort(String port) {
        return Pattern.matches("^[0-9]{1,5}$", port) && Integer.parseInt(port) <= 65535;
    }



    public void stopServer(ActionEvent ae) {
        setServerStopped();
    }

    public void exit(ActionEvent ae) {
        setServerStopped();
        System.exit(0);
    }

    private void showAlert(Alert.AlertType alertType, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Server Status");
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
