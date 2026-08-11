package motorph.ui.common;

import motorph.api.EmployeeApiClient;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.ui.session.CurrentUserSession;

import javafx.scene.control.Label;

/** Loads the logged-in employee's identity into sidebar labels. */
public final class SidebarHelper {

    // utility class — no instances
    private SidebarHelper() {
    }

    /**
     * Fetches the employee record and populates nameLabel, roleLabel, and — if non-null — emailLabel.
     * Falls back to the provided defaults on missing ID or error.
     * Pass {@code rolePrefix} as {@code ""} for plain role name, or {@code "Current Role: "} for IT style.
     */
    public static void load(Label nameLabel, Label roleLabel, Label emailLabel,
                            String fallbackName, String fallbackRole,
                            String rolePrefix,
                            EmployeeApiClient employeeApiClient) {
        int employeeId = CurrentUserSession.getEmployeeId();

        if (employeeId <= 0) {
            applyFallback(nameLabel, roleLabel, emailLabel, fallbackName, fallbackRole);
            return;
        }

        try {
            EmployeeProfileDTO employee = employeeApiClient.getEmployee(employeeId);

            String name  = employee.getFullName();
            String role  = rolePrefix + (CurrentUserSession.getActiveRole() != null
                    ? CurrentUserSession.getActiveRole().name()
                    : fallbackRole);
            String email = CurrentUserSession.getCompanyEmail() != null
                    ? CurrentUserSession.getCompanyEmail()
                    : "No email on file";

            setLabel(nameLabel, name);
            setLabel(roleLabel, role);
            if (emailLabel != null) setLabel(emailLabel, email);

        } catch (Exception e) {
            applyFallback(nameLabel, roleLabel, emailLabel, fallbackName, fallbackRole);
        }
    }

    // sets text and tags the label for the language system
    private static void setLabel(Label label, String text) {
        if (label == null) return;
        label.getProperties().put("englishText", text);
        label.setText(text);
    }

    private static void applyFallback(Label nameLabel, Label roleLabel, Label emailLabel,
                                      String fallbackName, String fallbackRole) {
        setLabel(nameLabel, fallbackName);
        setLabel(roleLabel, fallbackRole);
        if (emailLabel != null) setLabel(emailLabel, "No email on file");
    }
}
