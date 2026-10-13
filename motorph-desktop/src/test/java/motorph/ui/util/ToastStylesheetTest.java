package motorph.ui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the login screen loads the styles needed for toast messages.
 */
@DisplayName("Toast stylesheet Tests")
class ToastStylesheetTest {

    private static final String FEEDBACK_CSS = "css/core/feedback.css";

    // every scene root the app navigates to (see NavigationUtil and Main)
    @ParameterizedTest(name = "{0} loads the toast stylesheet")
    @DisplayName("every scene root should load the stylesheet that styles toasts")
    @ValueSource(strings = {
            "/motorph/ui/auth/login.fxml",
            "/motorph/ui/admin/admin-main.fxml",
            "/motorph/ui/employee/employee-main.fxml",
            "/motorph/ui/finance/finance-main.fxml",
            "/motorph/ui/hr/hr-main.fxml",
            "/motorph/ui/it/it-main.fxml"
    })
    void sceneRootShouldLoadToastStylesheet(String fxmlPath) throws Exception {
        List<String> stylesheets = rootStylesheets(fxmlPath);

        assertTrue(stylesheets.stream().anyMatch(sheet -> sheet.endsWith(FEEDBACK_CSS)),
                fxmlPath + " must load " + FEEDBACK_CSS + " or its toasts render unstyled, but it loads " + stylesheets);
    }

    // returns the entries of the root element's stylesheets attribute
    private List<String> rootStylesheets(String fxmlPath) throws Exception {
        try (InputStream fxml = ToastStylesheetTest.class.getResourceAsStream(fxmlPath)) {
            assertNotNull(fxml, "FXML not found: " + fxmlPath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            String attribute = factory.newDocumentBuilder()
                    .parse(fxml)
                    .getDocumentElement()
                    .getAttribute("stylesheets");

            return Arrays.stream(attribute.split(","))
                    .map(String::trim)
                    .filter(sheet -> !sheet.isEmpty())
                    .toList();
        }
    }
}
