package motorph.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;


/** Provides helpers for navigating between top-level FXML scenes. */
public final class NavigationUtil {

    // utility class — no instances
    private NavigationUtil() {
    }

    // replaces the scene root with the employee main shell
    public static void goToEmployeeApp(Scene scene) {
        replaceRoot(scene, "/motorph/ui/employee/employee-main.fxml");
    }

    // replaces the scene root with the auth login page
    public static void goToLogin(Scene scene) {
        replaceRoot(scene, "/motorph/ui/auth/login.fxml");
    }

    // loads an FXML and replaces the current scene root
    private static void replaceRoot(Scene scene, String fxmlPath) {
        try {
            var resource = NavigationUtil.class.getResource(fxmlPath);

            if (resource == null) {
                throw new RuntimeException("FXML not found: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(resource);
            scene.setRoot(root);
            ThemeManager.getInstance().applyCurrentTo(root);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }
}
