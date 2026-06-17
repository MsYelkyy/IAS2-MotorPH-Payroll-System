package motorph.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import motorph.ui.factory.EmployeeUIServiceFactory;

/**
 * Main entry point of the MotorPH JavaFX application.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize all application services before any FXML controller is loaded.
        EmployeeUIServiceFactory.initialize();

        Parent root = FXMLLoader.load(
                Main.class.getResource("/motorph/ui/auth/login.fxml")
        );

        Scene scene = new Scene(root, 1600, 900);

        stage.setTitle("MotorPH Payroll System");
        stage.setScene(scene);

        stage.setMinWidth(1366);
        stage.setMinHeight(768);

        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}