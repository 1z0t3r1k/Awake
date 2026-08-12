package com.amiawake.amiawake.common.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public String generateAccessToken(UUID userId) {
        Instant iat = Instant.now();
        Instant exp = iat.plus(properties.accessTokenTtl());
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder().subject(userId.toString()).issuedAt(iat).expiresAt(exp)
            .build();

        Jwt encodedJwt = jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet));

        return encodedJwt.getTokenValue();
    }
}
