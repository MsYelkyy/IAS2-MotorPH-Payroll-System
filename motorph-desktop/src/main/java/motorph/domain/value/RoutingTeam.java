package motorph.domain.value;

/**
 * Represents the team responsible for handling a support ticket.
 */
public enum RoutingTeam {

    HR,
    PAYROLL,
    IT,
    ADMIN;

    // converts text to routing team
    public static RoutingTeam fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Routing team is required.");
        }

        try {
            return RoutingTeam.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid routing team: " + value);
        }
    }
}