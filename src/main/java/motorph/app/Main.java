package motorph.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL loginView = Main.class.getResource("/fxml/login.fxml");
        if (loginView == null) {
            throw new IllegalStateException("Missing FXML resource: /fxml/login.fxml");
        }

        FXMLLoader loader = new FXMLLoader(loginView);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1360, 820);

        stage.setTitle("MotorPH AOOP");

        stage.setScene(scene);
        stage.setMinWidth(1120);
        stage.setMinHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
