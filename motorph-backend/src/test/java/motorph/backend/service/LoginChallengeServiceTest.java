package motorph.backend.service;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.LoginChallengeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginChallengeServiceTest {

    private static final String CHALLENGE_ID = "opaque-challenge";
    private static final int ACCOUNT_ID = 12;

    private LoginChallengeRepository repository;
    private LoginChallengeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LoginChallengeRepository.class);
        service = new LoginChallengeService(repository, 10);
        when(repository.findById(CHALLENGE_ID)).thenReturn(new LoginChallenge(
                CHALLENGE_ID, ACCOUNT_ID, LocalDateTime.now().plusMinutes(10), false, LocalDateTime.now()));
    }

    @Test
    void resolvingChallengeDoesNotConsumeIt() {
        assertThat(service.resolve(CHALLENGE_ID)).isEqualTo(ACCOUNT_ID);

        verify(repository).findById(CHALLENGE_ID);
        verify(repository, never()).markConsumed(CHALLENGE_ID);
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

        assertThat(service.resolveAndConsume(CHALLENGE_ID)).isEqualTo(ACCOUNT_ID);

        verify(repository).markConsumed(CHALLENGE_ID);
    }
}
