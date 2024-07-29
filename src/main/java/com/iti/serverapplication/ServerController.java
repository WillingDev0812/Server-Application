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
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.iti.serverapplication.ServerClientHandler.setAllUsersOffline;

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
    public static Map<Socket,String> ss =new HashMap<>(); ;
    public static List<Socket> sockets = new ArrayList<>();
    public static ServerSocket serverSocket;
    private Thread serverThread;
    private ScheduledExecutorService scheduler;
    private boolean isServerOnline = false; // Flag to track server status

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pieChart.setTitle("User Status");
        pieChart.setVisible(false);
        stopServerBtn.setVisible(false);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
        startPieChartUpdates(); // Ensure updates start on initialization
    }

    public synchronized void addSocket(Socket socket) {
        if (socket != null && !sockets.contains(socket)) {
            //sockets.add(socket);
        }
    }
    public synchronized void removeSocket(Socket socket) {
        sockets.remove(socket);
    }

    private void startPieChartUpdates() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::updatePieChart, 0, 3, TimeUnit.SECONDS);
        }
    }

    private void stopPieChartUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void updatePieChart() {
        if (isServerOnline) { // Only update if the server is online
            List<PieChart.Data> pieChartData = new ArrayList<>();

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe", "root", "قخخف");
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT status, COUNT(*) AS count FROM users GROUP BY status");
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    String status = resultSet.getString("status");
                    int count = resultSet.getInt("count");
                    PieChart.Data data = new PieChart.Data(status.toUpperCase() + " (" + count + ")", count);
                    pieChartData.add(data);
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
                        //sockets.add(clientSocket);
                       // ss.put("gg",clientSocket);
                        new Thread(new ServerClientHandler(clientSocket)).start();
                    }
                } catch (SocketException e) {
                    System.out.println("Server socket closed, stopping server thread.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            startPieChartUpdates();
            pieChart.setVisible(true);
            isServerOnline = true;
            updatePieChart(); // Ensure chart is updated when server starts
            setAllUsersOffline();
            stopServerBtn.setVisible(true);
            startServerBtn.setVisible(false);
            statusText.setTextFill(Color.GREEN);
            statusText.setText("Online");
            portField.setDisable(true);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Server Failed to start", "Database connection failed");
            e.printStackTrace();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Server Failed to start", "Failed to start server socket");
            e.printStackTrace();
        }
    }

    private boolean isValidPort(String port) {
        return Pattern.matches("^[0-9]{1,5}$", port) && Integer.parseInt(port) <= 65535;
    }

    public void stopServer(ActionEvent ae) {
        try {
            setAllUsersOffline();
            for (Socket socket : sockets) {
                if (!socket.isClosed()) {
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    pw.println("SERVER_STOPPED");
                    pw.flush();
                }
            }
            //nfs eli fu2 bs hashmap
//            for (Socket socket : ss.keySet()) {
//                if (!socket.isClosed()) {
//                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
//                    pw.flush();
//                }
//            }
            sockets.clear();
            ss.clear();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }
            DatabaseConnectionManager.disconnect();

        } catch (SQLException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Failed to stop server", "Error occurred while stopping the server");
            e.printStackTrace();
        }
        stopPieChartUpdates(); // Ensure updates are stopped
        pieChart.getData().clear(); // Clear the chart data
        isServerOnline = false; // Set server status to offline
        stopServerBtn.setVisible(false);
        startServerBtn.setVisible(true);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
        portField.setDisable(false);
    }

    public void exit(ActionEvent e) {
        stopServer(e);
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
