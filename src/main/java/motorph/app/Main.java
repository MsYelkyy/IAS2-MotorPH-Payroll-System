package motorph.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import motorph.ui.common.EmailPreviewDialog;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.factory.HrUIServiceFactory;
import motorph.ui.factory.ITUIServiceFactory;

/**
 * Main entry point of the MotorPH JavaFX application.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize all application services before any FXML controller is loaded.
        EmployeeUIServiceFactory.initialize();
        ITUIServiceFactory.initialize();
        HrUIServiceFactory.initialize();

        Parent root = FXMLLoader.load(
                Main.class.getResource("/motorph/ui/auth/login.fxml")
        );

        Scene scene = new Scene(root, 1360, 880);

        stage.setTitle("MotorPH Payroll System");
        stage.setScene(scene);

        stage.setMinWidth(1360);
        stage.setMinHeight(820);

        stage.centerOnScreen();
        stage.show();

        EmployeeUIServiceFactory.getInstance()
                .getEmailPreviewService()
                .addListener(preview -> Platform.runLater(() ->
                        EmailPreviewDialog.show(stage, preview, isLightMode(scene.getRoot()))));
    }

    private boolean isLightMode(Node root) {
        return root != null && root.getStyleClass().contains("light-mode");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
