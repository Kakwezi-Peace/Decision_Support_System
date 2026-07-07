package rw.ac.dss.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-not-for-production-use-1234567890";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);
        userDetails = User.withUsername("disp1").password("irrelevant").authorities("ROLE_DISPATCHER").build();
    }

    @Test
    void generateToken_producesTokenValidForItsOwnUser() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("disp1");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = User.withUsername("someone-else").password("x").authorities("ROLE_DISPATCHER").build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L);
        String token = shortLivedJwtService.generateToken(userDetails);

        Thread.sleep(20);

        assertThat(shortLivedJwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateToken(userDetails);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.isTokenValid(tampered, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-1234567890ab", 60_000L);
        String token = otherService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }
}
