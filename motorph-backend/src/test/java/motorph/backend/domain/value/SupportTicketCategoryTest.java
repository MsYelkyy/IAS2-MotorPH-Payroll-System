package motorph.backend.domain.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportTicketCategoryTest {

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @DisplayName("A known category is accepted whatever its case or surrounding spaces")
    @CsvSource({"PAYROLL,PAYROLL", "payroll,PAYROLL", "' Attendance ',ATTENDANCE", "account,ACCOUNT", "System,SYSTEM", "other,OTHER"})
    void knownCategoriesAreAccepted(String value, SupportTicketCategory expected) {
        assertThat(SupportTicketCategory.fromValue(value)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "category \"{0}\"")
    @DisplayName("An unknown category is refused with a fixed message that does not repeat the value")
    @ValueSource(strings = {"Banana", "<script>alert(1)</script>", "payroll; drop table", "letmein-s3cret"})
    void unknownCategoryMessageDoesNotEchoTheValue(String value) {
        assertThatThrownBy(() -> SupportTicketCategory.fromValue(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid support ticket category.")
                .message().doesNotContain(value);
    }

    @ParameterizedTest(name = "category \"{0}\"")
    @DisplayName("A missing or blank category is still reported as required")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankCategoryIsRequired(String value) {
        assertThatThrownBy(() -> SupportTicketCategory.fromValue(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Support ticket category is required.");
    }

    @Test
    @DisplayName("Every category the database can hold is still readable")
    void everyDeclaredCategoryRoundTrips() {
        for (SupportTicketCategory category : SupportTicketCategory.values()) {
            assertThat(SupportTicketCategory.fromValue(category.name())).isEqualTo(category);
        }
    }
}
