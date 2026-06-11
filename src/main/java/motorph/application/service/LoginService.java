package motorph.application.service;

import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;

/**
 * Defines login operations used by the UI layer.
 */
public interface LoginService {

    // authenticates user credentials and sends OTP when required
    LoginResultDTO authenticate(LoginRequestDTO request);

    // verifies login OTP and completes login
    LoginResultDTO verifyLoginOtp(int userAccountId, String rawOtp);

    // checks if a QA email preview is available
    boolean hasEmailPreview();

    // returns the latest QA email preview
    EmailPreviewDTO getLatestEmailPreview();
}