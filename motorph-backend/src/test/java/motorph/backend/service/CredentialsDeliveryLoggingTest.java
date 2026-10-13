package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A temporary password may reach the caller only through the preview response. It must never be
 * written to the application log, at any level, by any logger.
 */
class CredentialsDeliveryLoggingTest {

    private static final String PASSWORD = "Tmp-Sup3r-Secret-Pw!";
    private static final String COMPANY_EMAIL = "juan.cruz@motorph.com";
    private static final String PERSONAL_EMAIL = "juan.cruz@gmail.com";

    private Logger root;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> logged;

    @BeforeEach
    void captureAllLogging() {
        root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        originalLevel = root.getLevel();
        root.setLevel(Level.TRACE);

        logged = new ListAppender<>();
        logged.start();
        root.addAppender(logged);
    }

    @AfterEach
    void restoreLogging() {
        root.detachAppender(logged);
        root.setLevel(originalLevel);
    }

    private CredentialsPreview deliver(Supplier<CredentialsPreview> delivery) {
        CredentialsPreview preview = delivery.get();

        assertThat(logged.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains(PASSWORD));
        assertThat(logged.list)
                .extracting(ILoggingEvent::getThrowableProxy)
                .allMatch(proxy -> proxy == null || !proxy.getMessage().contains(PASSWORD));

        return preview;
    }

    @Test
    @DisplayName("New-account credentials are returned in the preview and never logged")
    void newAccountCredentialsAreNotLogged() {
        CredentialsPreview preview = deliver(() ->
                new EmployeeCredentialsDeliveryService(true)
                        .sendEmployeeAccountCredentials(PERSONAL_EMAIL, COMPANY_EMAIL, PASSWORD));

        assertThat(preview.temporaryPassword()).isEqualTo(PASSWORD);
        assertThat(preview.body()).contains(PASSWORD);
    }

    @Test
    @DisplayName("With preview off, new-account credentials are neither returned nor logged")
    void newAccountCredentialsAreNotLoggedWhenPreviewIsOff() {
        CredentialsPreview preview = deliver(() ->
                new EmployeeCredentialsDeliveryService(false)
                        .sendEmployeeAccountCredentials(PERSONAL_EMAIL, COMPANY_EMAIL, PASSWORD));

        assertThat(preview).isNull();
    }

    @Test
    @DisplayName("A reset password is returned in the preview and never logged")
    void resetPasswordIsNotLogged() {
        CredentialsPreview preview = deliver(() ->
                new PasswordResetDeliveryService(true)
                        .sendTemporaryPassword(PERSONAL_EMAIL, COMPANY_EMAIL, PASSWORD));

        assertThat(preview.temporaryPassword()).isEqualTo(PASSWORD);
        assertThat(preview.body()).contains(PASSWORD);
    }

    @Test
    @DisplayName("With preview off, a reset password is neither returned nor logged")
    void resetPasswordIsNotLoggedWhenPreviewIsOff() {
        CredentialsPreview preview = deliver(() ->
                new PasswordResetDeliveryService(false)
                        .sendTemporaryPassword(PERSONAL_EMAIL, COMPANY_EMAIL, PASSWORD));

        assertThat(preview).isNull();
    }

    @Test
    @DisplayName("The preview masks the password and message body if the object is ever printed")
    void previewToStringMasksThePassword() {
        CredentialsPreview preview = new PasswordResetDeliveryService(true)
                .sendTemporaryPassword(PERSONAL_EMAIL, COMPANY_EMAIL, PASSWORD);

        assertThat(preview.toString()).doesNotContain(PASSWORD).contains("temporaryPassword=***");
    }
}
