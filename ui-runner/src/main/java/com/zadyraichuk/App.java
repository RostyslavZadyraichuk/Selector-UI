package com.zadyraichuk;

import com.zadyraichuk.controller.SelectorUIController;
import com.zadyraichuk.general.PropertiesFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    public static final PropertiesFile PROPERTIES;

    private SelectorUIController controller;

    static {
        PROPERTIES = loadAppProperties();
    }

    private static PropertiesFile loadAppProperties() {
        URL path = App.class.getResource("/app.properties");
        File propertiesFile = new File(Objects.requireNonNull(path).getPath());
        return new PropertiesFile(propertiesFile);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
//        Thread.setDefaultUncaughtExceptionHandler(this::catchException);

        controller = setUpPrimaryStage(primaryStage);
        controller.init();
    }

    private SelectorUIController setUpPrimaryStage(Stage primaryStage) throws IOException {
        FXMLLoader loader =
            new FXMLLoader(Objects.requireNonNull(getClass().getResource("/ui/xml/selector.fxml")));
        Parent root = loader.load();
        primaryStage.setTitle("Wheel Selector");
        primaryStage.setScene(new Scene(root, 550, 650));
        primaryStage.setResizable(false);
        primaryStage.show();

        return loader.getController();
    }

    @Override
    public void stop() throws InterruptedException {
        if (controller != null) {
            controller.shutDown();
        }
    }

//    private void catchException(Thread t, Throwable e) {
//        System.out.println(e.getMessage());
////        e.printStackTrace();
//    }

}
