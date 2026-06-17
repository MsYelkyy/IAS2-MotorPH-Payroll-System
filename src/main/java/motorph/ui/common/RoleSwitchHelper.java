package motorph.ui.common;

import motorph.domain.value.Role;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;

import javafx.scene.Scene;
import javafx.scene.control.Label;

/** Handles role switching and navigation between the Employee, IT, and Admin shells. */
public final class RoleSwitchHelper {

    // utility class — no instances
    private RoleSwitchHelper() {
    }

    /**
     * Switches the session to {@code target} and navigates to its shell.
     * If the user does not hold the target role, shows {@code notFoundMessage} in {@code errorLabel}.
     * Pass {@code null} for either to fail silently.
     */
    public static void switchTo(Role target, Scene scene, Label errorLabel, String notFoundMessage) {
        if (!CurrentUserSession.hasRole(target)) {
            if (errorLabel != null && notFoundMessage != null) {
                UiMessageUtil.setError(errorLabel, notFoundMessage);
            }
            return;
        }

        CurrentUserSession.switchRole(target);
        navigate(target, scene);
    }

    /**
     * Switches to the first non-active secondary role (IT before ADMIN).
     * Used by the Employee shell where the user may hold multiple roles.
     */
    public static void switchToNextAvailable(Scene scene, Label errorLabel) {
        if (!CurrentUserSession.hasMultipleRoles()) {
            if (errorLabel != null) {
                UiMessageUtil.setError(errorLabel, "No other roles available to switch to.");
            }
            return;
        }

        if (CurrentUserSession.hasRole(Role.IT) && CurrentUserSession.getActiveRole() != Role.IT) {
            CurrentUserSession.switchRole(Role.IT);
            navigate(Role.IT, scene);
            return;
        }

        if (CurrentUserSession.hasRole(Role.ADMIN) && CurrentUserSession.getActiveRole() != Role.ADMIN) {
            CurrentUserSession.switchRole(Role.ADMIN);
            navigate(Role.ADMIN, scene);
            return;
        }

        if (errorLabel != null) {
            UiMessageUtil.setError(errorLabel, "No available role to switch to.");
        }
    }

    private static void navigate(Role role, Scene scene) {
        if (role == Role.EMPLOYEE) NavigationUtil.goToEmployeeApp(scene);
        else if (role == Role.IT)   NavigationUtil.goToITApp(scene);
        else if (role == Role.ADMIN) NavigationUtil.goToAdminApp(scene);
    }
}
