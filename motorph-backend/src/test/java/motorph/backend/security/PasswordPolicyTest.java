package motorph.backend.security;

import motorph.backend.exception.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsEightCharactersWithAnUppercaseLetterAndANumber() {
        assertThatCode(() -> PasswordPolicy.validate("BrandNew1")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Short1A", "alllowercase1", "NoNumberHere", "12345678"})
    void rejectsPasswordsThatBreakTheRules(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password)).isInstanceOf(ValidationException.class);
    }
}
