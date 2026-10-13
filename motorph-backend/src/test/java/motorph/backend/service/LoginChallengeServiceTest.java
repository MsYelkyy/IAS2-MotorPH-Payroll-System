package motorph.backend.service;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.LoginChallengeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginChallengeServiceTest {

    private static final String CHALLENGE_ID = "opaque-challenge";
    private static final int ACCOUNT_ID = 12;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private LoginChallengeRepository repository;
    private LoginChallengeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LoginChallengeRepository.class);
        service = new LoginChallengeService(repository, clock, 10, 10, 5);
        when(repository.findById(CHALLENGE_ID)).thenReturn(challenge(false, 0));
    }

    @Test
    void resolvingChallengeDoesNotConsumeIt() {
        assertThat(service.resolve(CHALLENGE_ID, ChallengePurpose.TOTP_LOGIN).getUserAccountId())
                .isEqualTo(ACCOUNT_ID);

        verify(repository).findById(CHALLENGE_ID);
        verify(repository, never()).markConsumed(CHALLENGE_ID);
    }

    @Test
    void resolvingChallengeRejectsADifferentPurpose() {
        assertThatThrownBy(() -> service.resolve(CHALLENGE_ID, ChallengePurpose.TOTP_SETUP))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Login challenge is invalid or expired.");
    }

    @Test
    void resolvingChallengeRejectsAConsumedOrExhaustedChallenge() {
        when(repository.findById(CHALLENGE_ID)).thenReturn(challenge(true, 0));
        assertThatThrownBy(() -> service.resolve(CHALLENGE_ID, ChallengePurpose.TOTP_LOGIN))
                .isInstanceOf(AuthenticationException.class);

        when(repository.findById(CHALLENGE_ID)).thenReturn(challenge(false, LoginChallengeService.MAX_ATTEMPTS));
        assertThatThrownBy(() -> service.resolve(CHALLENGE_ID, ChallengePurpose.TOTP_LOGIN))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void consumingChallengeRejectsAConcurrentOrRepeatedUse() {
        when(repository.markConsumed(CHALLENGE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.consume(CHALLENGE_ID))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Login challenge is invalid or expired.");
    }

    @Test
    void resolveAndConsumeStillConsumesChallengesForSingleStepFlows() {
        when(repository.markConsumed(CHALLENGE_ID)).thenReturn(true);

        assertThat(service.resolveAndConsume(CHALLENGE_ID, ChallengePurpose.TOTP_LOGIN)).isEqualTo(ACCOUNT_ID);

        verify(repository).markConsumed(CHALLENGE_ID);
    }

    private LoginChallenge challenge(boolean consumed, int attempts) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new LoginChallenge(CHALLENGE_ID, ACCOUNT_ID, ChallengePurpose.TOTP_LOGIN,
                now.plusMinutes(5), consumed, attempts, null, null, now);
    }
}
