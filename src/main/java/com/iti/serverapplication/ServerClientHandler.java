package com.iti.serverapplication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerClientHandler implements Runnable {

    private final Socket socket;

    public ServerClientHandler(Socket socket) {
        this.socket = socket;
    }

    record user(String username, String status) {
    }
    List<user> users = new ArrayList<>();

    @Override
    public void run() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                String action = input.readUTF();
                if (action == null || action.isEmpty()) {
                    break; // Exit loop if no action is received
                }

                System.out.println("Action received: " + action);

                String username;
                String invitedUsername;
                String invitedStatus;
                String email;
                String password;
                boolean success;

                switch (action) {
                    case "login":
                        email = input.readUTF();
                        password = input.readUTF();
                        success = checkLogin(email, password);
                        output.writeBoolean(success);
                        if (success) {
                            updateStatus(email, "online");
                        }
                        break;
                    case "signup":
                        username = input.readUTF();
                        email = input.readUTF();
                        password = input.readUTF();
                        success = registerUser(username, email, password);
                        output.writeBoolean(success);
                        break;
                    case "showUsers":
                        email = input.readUTF();
                        output.writeUTF(getUsername(email)); // send username to client
                        this.users = getUsers(email);
                        output.writeInt(users.size());
                        for (user u : users) {
                            output.writeUTF(u.username() + "   " + u.status());
                        }
                        output.flush();
                        break;
                    case "offline":
                        email = input.readUTF();
                        updateStatus(email, "offline");
                        break;
                    case "invite":
                        invitedUsername = input.readUTF();
                        invitedStatus = getUserStatus(invitedUsername);
                        output.writeUTF(invitedStatus);
                        break;
                    default:
                        output.writeUTF("Invalid action");
                        break;
                }
            }
        } catch (EOFException e) {
            System.err.println("Connection closed unexpectedly: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } finally {
            try {
                socket.close(); // Ensure the socket is closed properly
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private String userState(String username) {
        System.out.println("Retrieving status for username: " + username);
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT status FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String status = resultSet.getString("status");
                    System.out.println("Status for " + username + ": " + status);
                    return status;
                } else {
                    System.out.println("User not found, returning offline");
                    return "offline";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error";
        }
    }





    private boolean checkLogin(String username, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean updateStatus(String email, String status) throws SQLException {
        System.out.println("Updating status for email: " + email + " to " + status);
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "UPDATE users SET status = ? WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, status);
            statement.setString(2, email);
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        }
    }




    private boolean registerUser(String username, String email, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "INSERT INTO users (username, email, password, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, password);
            statement.setString(4, "Offline");
            return statement.executeUpdate() > 0;
        }
    }

    private List<user> getUsers(String email) throws SQLException {
        List<user> users = new ArrayList<>();
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email != ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    String status = resultSet.getString("status");
                    users.add(new user(username, status));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }



    private String getUsername(String email) {
        String username = "Player"; // Default value
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DatabaseConnectionManager.getConnection();
            String query = "SELECT username FROM users WHERE email = ?"; // Use = for comparison
            statement = connection.prepareStatement(query);
            statement.setString(1, email);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                username = resultSet.getString("username");
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        return username;
    }

    private String getUserStatus(String invitedUsername) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String status = "Unknown"; // Default status if the user is not found

        try {
            // Get the database connection
            connection = DatabaseConnectionManager.getConnection();
            // Prepare the SQL query
            String query = "SELECT username, status FROM users WHERE username = ?";
            statement = connection.prepareStatement(query);
            // Set the username parameter
            statement.setString(1, invitedUsername);
            // Execute the query
            resultSet = statement.executeQuery();

            // Process the result
            if (resultSet.next()) {
                status = resultSet.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle exceptions
        } finally {
            // Clean up resources
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return status;
    }

}