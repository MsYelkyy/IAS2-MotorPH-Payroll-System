package motorph.domain.model;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests government number format validation.
 */
@DisplayName("GovernmentDetails Tests")
class GovernmentDetailsTest {


    private static final Logger logger = Logger.getLogger(GovernmentDetailsTest.class.getName());


    // accepts valid government numbers
    @Test
    @DisplayName("Positive - constructor should accept valid government numbers")
    void constructorShouldAcceptValidNumbers() {
        logger.info("Testing valid government numbers are accepted.");

        GovernmentDetails details = new GovernmentDetails(
                1, 10001, "44-4506057-3", "442-605-657-000", "691295330870", "820126853951");

        assertEquals("44-4506057-3", details.getSssNumber());
        assertEquals("442-605-657-000", details.getTinNumber());
    }


    // rejects an invalid TIN format
    @Test
    @DisplayName("Negative - constructor should reject an invalid TIN format")
    void constructorShouldRejectInvalidTin() {
        logger.info("Testing invalid TIN format validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new GovernmentDetails(
                        1, 10001, "44-4506057-3", "ABC-123", "691295330870", "820126853951")
        );
    }


    // rejects an invalid PhilHealth format
    @Test
    @DisplayName("Negative - constructor should reject an invalid PhilHealth format")
    void constructorShouldRejectInvalidPhilhealth() {
        logger.info("Testing invalid PhilHealth format validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new GovernmentDetails(
                        1, 10001, "44-4506057-3", "442-605-657-000", "691295330870", "bad#value")
        );
    }


    // rejects an invalid Pag-IBIG format
    @Test
    @DisplayName("Negative - constructor should reject an invalid Pag-IBIG format")
    void constructorShouldRejectInvalidPagibig() {
        logger.info("Testing invalid Pag-IBIG format validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new GovernmentDetails(
                        1, 10001, "44-4506057-3", "442-605-657-000", "12ab34", "820126853951")
        );
    }


    // rejects a blank SSS number
    @Test
    @DisplayName("Negative - constructor should reject a blank SSS number")
    void constructorShouldRejectBlankSss() {
        logger.info("Testing blank SSS number validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new GovernmentDetails(
                        1, 10001, "", "442-605-657-000", "691295330870", "820126853951")
        );
    }
}
