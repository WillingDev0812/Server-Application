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
    List<String> users = new ArrayList<>();
    @Override
    public void run() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String action = input.readUTF();
            System.out.println("Action received: " + action);

            String username;
            String email;
            String password;
            boolean success;

            switch (action) {
                case "login":
                    email = input.readUTF();
                    //username = input.readUTF();
                    password = input.readUTF();
                    //System.out.println("Login request - Email: " + email + ", Username: " + username);
                    success = checkLogin(email, password);
                    output.writeBoolean(success);
                    break;
                case "signup":
                    username = input.readUTF();
                    email = input.readUTF();
                    password = input.readUTF();
                    System.out.println("Sign-up request - Username: " + username + ", Email: " + email);
                    success = registerUser(username, email, password);
                    output.writeBoolean(success);
                    break;
                case "showUsers":
                    // Read email from client before calling getUsernames
                    email = input.readUTF();
                    output.writeUTF(getUsername(email)); //send username to client
                    getUsernames(email);
                    output.writeInt(users.size());
                    System.out.println(users.size());
                    for(int i=0;i<users.size();i++)
                    {
                        output.writeUTF(users.get(i));
                    }
                    output.flush();
                    break;
                default:
                    output.writeUTF("Invalid action");
                    break;
            }

        } catch (EOFException e) {
            System.err.println("Connection closed unexpectedly: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
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

    private boolean registerUser(String username, String email, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, password);
            return statement.executeUpdate() > 0;
        }
    }

    private void getUsernames(String email) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT username FROM users WHERE email != ?";
        System.out.println("a7a"+email);
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                users.clear();
                while (resultSet.next()) {
                    users.add(resultSet.getString("username"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
}