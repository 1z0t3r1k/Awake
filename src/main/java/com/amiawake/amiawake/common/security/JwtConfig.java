package com.amiawake.amiawake.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {
    private final JwtProperties properties;

    public JwtConfig(JwtProperties properties) {
        this.properties = properties;
    }

    private RSAPrivateKey readPrivateKey(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        }
    }

    private RSAPublicKey readPublicKey(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder() throws IOException {
        RSAPublicKey publicKey = readPublicKey(properties.publicKey());
        RSAPrivateKey privateKey = readPrivateKey(properties.privateKey());

        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    @Bean
    public JwtDecoder jwtDecoder() throws IOException {
        RSAPublicKey publicKey = readPublicKey(properties.publicKey());

        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}

