package motorph.ui.session;

import motorph.domain.value.Role;
import motorph.application.session.UserSession;

import java.util.List;

/** Provides UI-layer access to the current logged-in user's session state. */
public final class CurrentUserSession {

    // utility class — no instances
    private CurrentUserSession() {
    }

    // returns the current employee ID, or 0 if not logged in
    public static int getEmployeeId() {
        return UserSession.getInstance().getEmployeeId();
    }

    // returns the current user account ID, or 0 if not set
    public static int getUserAccountId() {
        Integer id = UserSession.getInstance().getUserAccountId();
        return id != null ? id : 0;
    }

    // returns the current user's company email, or null if not set
    public static String getCompanyEmail() {
        return UserSession.getInstance().getCompanyEmail();
    }

    // returns true when a real employee is logged in (not offline preview)
    public static boolean isLoggedIn() {
        return UserSession.getInstance().isLoggedIn() && getEmployeeId() > 0;
    }

    // returns true when the session is in offline/preview mode (no real employee ID)
    public static boolean isOfflinePreview() {
        return UserSession.getInstance().isLoggedIn() && getEmployeeId() == 0;
    }

    // returns the current active role, or null if not set
    public static Role getActiveRole() {
        return UserSession.getInstance().getActiveRole();
    }

    public static List<Role> getRoles() {
        return UserSession.getInstance().getRoles();
    }

    public static boolean hasRole(Role role) {
        return UserSession.getInstance().hasRole(role);
    }

    public static boolean hasMultipleRoles() {
        return UserSession.getInstance().hasMultipleRoles();
    }

    public static void switchRole(Role role) {
        UserSession.getInstance().switchRole(role);
    }

    // clears all current session data
    public static void clear() {
        UserSession.getInstance().clear();
    }
}
