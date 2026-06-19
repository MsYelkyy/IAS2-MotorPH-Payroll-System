package motorph.ui.common;

import motorph.domain.value.Role;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import java.util.ArrayList;
import java.util.List;

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
     * Cycles to the next role in the user's assigned role list (wraps around).
     * Works for any number of roles without hardcoding role names.
     */
    public static void switchToNext(Scene scene, Label errorLabel) {
        List<Role> roles = new ArrayList<>(CurrentUserSession.getRoles());
        if (roles.size() <= 1) {
            if (errorLabel != null) {
                UiMessageUtil.setError(errorLabel, "No other roles available to switch to.");
            }
            return;
        }

        Role active = CurrentUserSession.getActiveRole();
        int idx = roles.indexOf(active);
        Role next = roles.get((idx + 1) % roles.size());
        CurrentUserSession.switchRole(next);
        navigate(next, scene);
    }

    private static void navigate(Role role, Scene scene) {
        if (role == Role.EMPLOYEE)                      NavigationUtil.goToEmployeeApp(scene);
        else if (role == Role.IT)                       NavigationUtil.goToITApp(scene);
        else if (role == Role.ADMIN || role == Role.HR) NavigationUtil.goToAdminApp(scene);
    }
}
