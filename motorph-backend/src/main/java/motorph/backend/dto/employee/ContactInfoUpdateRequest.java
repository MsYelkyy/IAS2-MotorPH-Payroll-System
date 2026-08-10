package motorph.backend.dto.employee;

/** The contact fields an employee may change on their own profile. */
public record ContactInfoUpdateRequest(
        String phoneNumber,
        String address
) {
}
