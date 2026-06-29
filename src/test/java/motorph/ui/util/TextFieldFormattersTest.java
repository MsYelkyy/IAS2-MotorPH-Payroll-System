package motorph.ui.util;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests text field formatting and masking logic.
 */
@DisplayName("TextFieldFormatters Tests")
class TextFieldFormattersTest {


    // formats raw digits into a phone number
    @Test
    @DisplayName("maskPhone should format digits as XXX-XXX-XXXX")
    void maskPhoneShouldFormatDigits() {
        assertEquals("917-123-4567", TextFieldFormatters.maskPhone("9171234567"));
    }


    // strips non-digits and caps phone length at ten digits
    @Test
    @DisplayName("maskPhone should strip symbols and cap at ten digits")
    void maskPhoneShouldStripAndCap() {
        assertEquals("917-123-4567", TextFieldFormatters.maskPhone("(917) 123-4567 89"));
    }


    // returns empty for null phone input
    @Test
    @DisplayName("maskPhone should return empty for null")
    void maskPhoneShouldReturnEmptyForNull() {
        assertEquals("", TextFieldFormatters.maskPhone(null));
    }


    // formats raw digits into a date
    @Test
    @DisplayName("maskDate should format digits as XX-XX-XXXX")
    void maskDateShouldFormatDigits() {
        assertEquals("01-15-2025", TextFieldFormatters.maskDate("01152025"));
    }


    // formats a government id using the given dash positions
    @Test
    @DisplayName("maskGovId should place dashes at the given positions")
    void maskGovIdShouldPlaceDashes() {
        // dashes sit at output positions 2 and 9, digits fill the rest
        assertEquals("44-450605-73", TextFieldFormatters.maskGovId("4445060573", new int[]{2, 9}, 12));
    }


    // accepts digits and dashes within the digit limit
    @Test
    @DisplayName("isValidDigitMask should accept digits and dashes within limit")
    void isValidDigitMaskShouldAcceptValid() {
        assertTrue(TextFieldFormatters.isValidDigitMask("44-450605", 10));
    }


    // rejects input with letters
    @Test
    @DisplayName("isValidDigitMask should reject letters")
    void isValidDigitMaskShouldRejectLetters() {
        assertFalse(TextFieldFormatters.isValidDigitMask("44a", 10));
    }


    // rejects input exceeding the digit limit
    @Test
    @DisplayName("isValidDigitMask should reject too many digits")
    void isValidDigitMaskShouldRejectTooManyDigits() {
        assertFalse(TextFieldFormatters.isValidDigitMask("12345678901", 10));
    }
}
