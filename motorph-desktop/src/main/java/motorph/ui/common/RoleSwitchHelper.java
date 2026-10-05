package motorph.ui.common;

import motorph.domain.value.Role;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        Role next = nextRole(roles);
        CurrentUserSession.switchRole(next);
        navigate(next, scene);
    }

    /** Name of the role {@link #switchToNext} would move to, e.g. "HR" or "Admin", for the button label. */
    public static String nextRoleLabel() {
        List<Role> roles = new ArrayList<>(CurrentUserSession.getRoles());
        if (roles.size() <= 1) {
            return "role";
        }

        Role next = nextRole(roles);
        return next == Role.HR || next == Role.IT
                ? next.name()
                : next.name().charAt(0) + next.name().substring(1).toLowerCase(Locale.ROOT);
    }

    // the role after the active one in the user's role list (wraps around)
    private static Role nextRole(List<Role> roles) {
        int idx = roles.indexOf(CurrentUserSession.getActiveRole());
        return roles.get((idx + 1) % roles.size());
    }

    private static void navigate(Role role, Scene scene) {
        if (role == Role.EMPLOYEE)         NavigationUtil.goToEmployeeApp(scene);
        else if (role == Role.IT)          NavigationUtil.goToITApp(scene);
        else if (role == Role.HR)          NavigationUtil.goToHrApp(scene);
        else if (role == Role.ADMIN)       NavigationUtil.goToAdminApp(scene);
        else if (role == Role.FINANCE)     NavigationUtil.goToFinanceApp(scene);
    }
}
