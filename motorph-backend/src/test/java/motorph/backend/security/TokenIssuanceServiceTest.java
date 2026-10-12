package motorph.backend.security;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A refresh token only yields a new access token while its account is still active and unlocked,
 * so disabling or locking an account stops the session from renewing itself.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenIssuanceServiceTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;

    @Mock private TokenRevocationRepository tokenRevocationRepository;
    @Mock private UserAccountRepository userAccountRepository;

    private JwtTokenProvider jwtTokenProvider;
    private TokenIssuanceService tokenIssuanceService;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "control-two-test-signing-secret-with-at-least-32-bytes", 15, 1440);
        tokenIssuanceService = new TokenIssuanceService(
                jwtTokenProvider, tokenRevocationRepository, userAccountRepository);

        refreshToken = jwtTokenProvider.generateRefreshToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, true));
    }

    @Test
    @DisplayName("An active, unlocked account can refresh and keeps the same refresh token")
    void activeAccountCanRefresh() {
        TokenResponse response = tokenIssuanceService.refresh(refreshToken);

        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(jwtTokenProvider.isAccessToken(jwtTokenProvider.parseClaims(response.accessToken()))).isTrue();
        verify(userAccountRepository).findById(USER_ACCOUNT_ID);
    }

    @Test
    @DisplayName("A locked account cannot refresh")
    void lockedAccountCannotRefresh() {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));

        assertThatThrownBy(() -> tokenIssuanceService.refresh(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("An inactive account cannot refresh")
    void inactiveAccountCannotRefresh() {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));

        assertThatThrownBy(() -> tokenIssuanceService.refresh(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("A locked and inactive account cannot refresh")
    void lockedAndInactiveAccountCannotRefresh() {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, false));

        assertThatThrownBy(() -> tokenIssuanceService.refresh(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("An account that no longer exists cannot refresh")
    void missingAccountCannotRefresh() {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(null);

        assertThatThrownBy(() -> tokenIssuanceService.refresh(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("The refusal does not reveal whether the account is locked, inactive, or missing")
    void refusalMessageIsTheSameForEveryReason() {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));
        String locked = refreshFailureMessage();

        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));
        String inactive = refreshFailureMessage();

        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(null);
        String missing = refreshFailureMessage();

        assertThat(locked).isEqualTo(inactive).isEqualTo(missing);
        assertThat(locked).doesNotContainIgnoringCase("locked").doesNotContainIgnoringCase("inactive");
    }

    @Test
    @DisplayName("A revoked refresh token is still refused")
    void revokedRefreshTokenIsRefused() {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(true);

        assertThatThrownBy(() -> tokenIssuanceService.refresh(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("An access token cannot be exchanged as a refresh token")
    void accessTokenCannotBeRefreshed() {
        String accessToken = jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

        assertThatThrownBy(() -> tokenIssuanceService.refresh(accessToken))
                .isInstanceOf(AuthenticationException.class);
    }

    private String refreshFailureMessage() {
        try {
            tokenIssuanceService.refresh(refreshToken);
        } catch (AuthenticationException e) {
            return e.getMessage();
        }
        throw new AssertionError("Refresh should have been refused.");
    }

    private UserAccount account(boolean locked, boolean active) {
        return new UserAccount(
                USER_ACCOUNT_ID,
                EMPLOYEE_ID,
                "employee@motorph.com",
                "employee@motorph.com",
                "password-hash",
                "password-salt",
                List.of(Role.EMPLOYEE),
                locked,
                active,
                null,
                false,
                null
        );
    }
}
