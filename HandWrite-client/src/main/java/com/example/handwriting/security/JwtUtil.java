package com.example.handwriting.security;

import com.example.handwriting.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具：生成、解析、校验。
 * 签名算法：HS256。密钥从 Base64 字符串解码。
 */
@Component
public class JwtUtil {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "usr";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMS = "perms";
    public static final String CLAIM_TOKEN_TYPE = "type";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final AppProperties appProperties;
    private final SecretKey secretKey;

    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
        byte[] bytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /** 生成 access token */
    public String generateAccessToken(Long userId, String username, List<String> roles, List<String> perms) {
        return buildToken(userId, username, roles, perms, TYPE_ACCESS, appProperties.getJwt().getAccessTokenTtlSeconds());
    }

    /** 生成 refresh token */
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, List.of(), List.of(), TYPE_REFRESH, appProperties.getJwt().getRefreshTokenTtlSeconds());
    }

    private String buildToken(Long userId, String username, List<String> roles, List<String> perms, String type, long ttlSeconds) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLES, roles);
        claims.put(CLAIM_PERMS, perms);
        claims.put(CLAIM_TOKEN_TYPE, type);
        return Jwts.builder()
                .claims(claims)
                .issuer(appProperties.getJwt().getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }

    /** 解析 token，返回 Claims；解析失败抛出 JwtException */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(appProperties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /** 校验 token 有效性（不抛异常） */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getAccessTtlSeconds() {
        return appProperties.getJwt().getAccessTokenTtlSeconds();
    }

    public long getRefreshTtlSeconds() {
        return appProperties.getJwt().getRefreshTokenTtlSeconds();
    }
}
