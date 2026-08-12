package com.amiawake.amiawake.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(Resource privateKey, Resource publicKey, Duration accessTokenTtl) {
}
