package com.geollm.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    private final Key key;

    public JwtUtil(String secret) {
        // jjwt 要求 key 足够长；不足则会抛异常。这里简单做填充以兼容你的旧配置。
        String s = secret == null ? "" : secret;
        if (s.length() < 32) {
            s = (s + "00000000000000000000000000000000").substring(0, 32);
        }
        this.key = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Map<String, Object> claims, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

