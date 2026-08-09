package motorph.backend.dto.auth;

/**
 * Wire request body for POST /api/v1/auth/logout. The refresh token is
 * optional in the body but should be supplied whenever the client holds
 * one, so both the access and refresh token are revoked together - otherwise
 * a copied refresh token would keep minting new access tokens post-logout.
 */
public record LogoutRequest(String refreshToken) {
}
