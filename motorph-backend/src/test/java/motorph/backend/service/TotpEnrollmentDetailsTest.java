package motorph.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpEnrollmentDetailsTest {

    @Test
    void comparesByContentAndHidesTheSecret() {
        TotpService.EnrollmentDetails first = new TotpService.EnrollmentDetails(new byte[]{1, 2}, "ABCD EFGH", "juan@motorph.com");
        TotpService.EnrollmentDetails same = new TotpService.EnrollmentDetails(new byte[]{1, 2}, "ABCD EFGH", "juan@motorph.com");
        TotpService.EnrollmentDetails different = new TotpService.EnrollmentDetails(new byte[]{1, 3}, "ABCD EFGH", "juan@motorph.com");

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(different);
        assertThat(first.toString()).doesNotContain("ABCD").contains("juan@motorph.com");
    }
}
