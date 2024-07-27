package com.iti.serverapplication;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    @FXML
    private PieChart pieChart;

    public static List<Socket> sockets = new ArrayList<>();
    private ServerSocket serverSocket;
    private Thread serverThread;
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stopServerBtn.setVisible(false);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
        pieChart.setTitle("User Status");
        startPieChartUpdates();
        pieChart.setVisible(false);
    }

    private void startPieChartUpdates() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updatePieChart, 0, 5, TimeUnit.SECONDS);
    }

    private void stopPieChartUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void updatePieChart() {
        List<PieChart.Data> pieChartData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe", "root", "new_password");
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT status, COUNT(*) AS count FROM users GROUP BY status");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String status = resultSet.getString("status");
                int count = resultSet.getInt("count");
                pieChartData.add(new PieChart.Data(status, count));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Update on JavaFX Application Thread
        Platform.runLater(() -> {
            pieChart.getData().clear(); // Clear existing data
            pieChart.getData().addAll(pieChartData);
        });
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
                        Socket clientSocket = serverSocket.accept();
                        sockets.add(clientSocket);
                        new Thread(new ServerClientHandler(clientSocket)).start();
                    }
                } catch (SocketException e) {
                    System.out.println("Server socket closed, stopping server thread.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            pieChart.setVisible(true);
            setAllUsersOffline();
            stopServerBtn.setVisible(true);
            startServerBtn.setVisible(false);
            statusText.setTextFill(Color.GREEN);
            statusText.setText("Online");
            portField.setDisable(true);
            showAlert(Alert.AlertType.CONFIRMATION, "Server Started", "Server started successfully");
            updatePieChart();
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
            for (Socket socket : sockets) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
            sockets.clear();
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
        updatePieChart();
        stopPieChartUpdates();
    }

    public void exit(ActionEvent ae) {
        setServerStopped();
        stopPieChartUpdates();
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
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe", "root", "new_password");
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE users SET status = 'offline'")) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
