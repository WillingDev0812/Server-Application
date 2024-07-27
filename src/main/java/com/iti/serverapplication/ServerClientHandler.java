package com.iti.serverapplication;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.iti.serverapplication.ServerController.sockets;

public class ServerClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();

    public ServerClientHandler(Socket socket) {
        this.socket = socket;
    }

    static class User {
        private String username;
        private String status;

        public User(String username, String status) {
            this.username = username;
            this.status = status;
        }

        // Getters and Setters
    }

    static class LoginRequest {
        private String action;
        private String email;
        private String password;

        // Getters and Setters
    }

    static class SignupRequest {
        private String action;
        private String username;
        private String email;
        private String password;

        // Getters and Setters
    }

    static class ShowUsersRequest {
        private String action;
        private String email;

        // Getters and Setters
    }

    static class InviteRequest {
        private String action;
        @SerializedName("player")
        private String invitedUsername;
        // Getters and Setters
    }

    static class GenericResponse {
        private boolean success;
        private String message;

        public GenericResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
    }

    List<User> users = new ArrayList<>();

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String requestJson;
            while ((requestJson = input.readLine()) != null) {
                System.out.println("Received JSON: " + requestJson);
                try {
                    Map<String, Object> requestMap = gson.fromJson(requestJson, Map.class);
                    String action = (String) requestMap.get("action");

                    String responseJson;
                    switch (action) {
                        case "login" -> {
                            System.out.println("the sockets = " +sockets.size());
                            System.out.println("Login");
                            LoginRequest loginRequest = gson.fromJson(requestJson, LoginRequest.class);
                            boolean success = checkLogin(loginRequest.email, loginRequest.password);
                            if (success) {
                                updateStatus(loginRequest.email, "online");
                            }
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Login successful" : "Login failed"));
                        }
                        case "signup" -> {
                            System.out.println("Signup");
                            SignupRequest signupRequest = gson.fromJson(requestJson, SignupRequest.class);
                            boolean success = registerUser(signupRequest.username, signupRequest.email, signupRequest.password);
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Signup successful" : "Signup failed"));
                        }
                        case "showUsers" -> {
                            ShowUsersRequest showUsersRequest = gson.fromJson(requestJson, ShowUsersRequest.class);
                            users = getUsers(showUsersRequest.email);
                            responseJson = gson.toJson(users);
                        }
                        case "offline" -> {
                            String email = (String) requestMap.get("email");
                            updateStatus(email, "offline");
                            responseJson = gson.toJson(new GenericResponse(true, "Status updated to offline"));
                        }
                        case "invite" -> {
                            for(Socket s : sockets)
                                System.out.println("the sockets when invite = " +s);
                            System.out.println("the sockets when invite = " +sockets.size());
                            InviteRequest inviteRequest = gson.fromJson(requestJson, InviteRequest.class);
                            String invitedStatus = getUserStatus(inviteRequest.invitedUsername);
                            System.out.println("Invited status: " + invitedStatus);
                            if(invitedStatus.equals("offline"))
                                responseJson = gson.toJson(new GenericResponse(true, invitedStatus));
                            else if (invitedStatus.equals("ingame"))
                                responseJson = gson.toJson(new GenericResponse(true, invitedStatus));
                            else {    //case online
                                responseJson = gson.toJson(new GenericResponse(true, "invite sent "));
                                for(Socket s : sockets){

                                }
                            }
                        }
                        default -> {
                            responseJson = gson.toJson(new GenericResponse(false, "Invalid action"));
                        }
                    }

                    // Send the response
                    output.println(responseJson);
                    output.flush(); // Ensure the response is sent

                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    String errorResponse = gson.toJson(new GenericResponse(false, "Invalid JSON format"));
                    output.println(errorResponse);
                    output.flush(); // Ensure the error response is sent
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkLogin(String email, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean updateStatus(String email, String status) throws SQLException {
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
            statement.setString(4, "offline");
            return statement.executeUpdate() > 0;
        }
    }

    private List<User> getUsers(String email) throws SQLException {
        List<User> users = new ArrayList<>();
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email != ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    String status = resultSet.getString("status");
                    users.add(new User(username, status));
                }
            }
        }
        return users;
    }

    private String getUsername(String email) {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT username FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Player";
    }

    private String getUserStatus(String invitedUsername) {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT status FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, invitedUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "offline";
    }
}
