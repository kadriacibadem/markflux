package com.markflux.gateway.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "markflux.dev-token.enabled", havingValue = "true")
public class DevTokenController {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;

    public DevTokenController(JwtEncoder jwtEncoder, @Value("${markflux.dev-token.ttl:1h}") Duration tokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
    }

    @PostMapping("/auth/token")
    public TokenResponse createToken(@Valid @RequestBody TokenRequest request) {
        Set<DevRole> roles = request.roles() == null || request.roles().isEmpty()
                ? Set.of(DevRole.CUSTOMER)
                : request.roles();
        List<String> roleNames = roles.stream().map(Enum::name).sorted().toList();
        UUID userId = userIdOf(request.username());
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .claim("preferred_username", request.username())
                .claim("roles", roleNames)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", tokenTtl.toSeconds(), userId, request.username(), roleNames);
    }

    static UUID userIdOf(String username) {
        return UUID.nameUUIDFromBytes(("markflux:user:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public record TokenRequest(@NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
            Set<DevRole> roles) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, UUID userId, String username,
            List<String> roles) {
    }
}
