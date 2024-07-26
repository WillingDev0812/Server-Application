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
import java.net.SocketException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
    private Thread serverThread;

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
            serverThread = new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        new Thread(new ServerClientHandler(serverSocket.accept())).start();
                    }
                } catch (SocketException e) {
                    System.out.println("Server socket closed, stopping server thread.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            setAllUsersOffline();
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
            setAllUsersOffline();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThread != null) {
                serverThread.interrupt();
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

    public void setAllUsersOffline() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe", "root", "root");
            String updateQuery = "UPDATE users SET status = 'offline'";
            preparedStatement = connection.prepareStatement(updateQuery);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
