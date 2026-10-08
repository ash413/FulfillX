package com.fulfillx.orderservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtService {

    public static final Duration TTL = Duration.ofHours(1);

    private final JwtEncoder encoder;

    public String issue(AppUser user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fulfillx-order-service")
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}