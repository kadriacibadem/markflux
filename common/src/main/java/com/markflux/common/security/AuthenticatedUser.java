package com.markflux.common.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(UUID id, String username, Set<String> roles) {

    public static AuthenticatedUser from(Jwt jwt) {
        List<String> roles = jwt.hasClaim("roles") ? jwt.getClaimAsStringList("roles") : List.of();
        String username = jwt.hasClaim("preferred_username") ? jwt.getClaimAsString("preferred_username")
                : jwt.getSubject();
        return new AuthenticatedUser(parseUserId(jwt.getSubject()), username, Set.copyOf(roles));
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    private static UUID parseUserId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadCredentialsException("Token subject is not a valid user id");
        }
    }
}
