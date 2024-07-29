package com.iti.serverapplication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static com.iti.serverapplication.ServerClientHandler.setAllUsersOffline;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/com/iti/serverapplication/Server.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setFullScreen(false);
        stage.setTitle("Server ");
        stage.setScene(scene);
        stage.show();
    }
    @Override
    public void stop() throws Exception {
        setAllUsersOffline();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}