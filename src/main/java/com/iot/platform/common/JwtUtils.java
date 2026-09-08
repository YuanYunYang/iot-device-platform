package com.iot.platform.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 基于 JJWT 0.12.6 实现 Token 的生成、解析与校验。
 * Token 中携带用户名与角色信息，用于后续接口鉴权。
 *
 * @author iot-platform
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:iotPlatformJwtSecretKey2026VeryLongSecretString}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // 密钥至少 32 字节，满足 HS256 算法要求
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 工具初始化完成, 过期时间={}ms", expiration);
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @param role     角色
     * @param tenantId 租户ID
     * @return JWT Token 字符串
     */
    public String generateToken(String username, String role, Long tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     *
     * @param token JWT Token
     * @return Claims 载荷
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token
     * @return 角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从 Token 中获取租户 ID
     *
     * @param token JWT Token
     * @return 租户 ID
     */
    public Long getTenantIdFromToken(String token) {
        Object tenantId = parseToken(token).get("tenantId");
        if (tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        return null;
    }

    /**
     * 判断 Token 是否已过期
     *
     * @param token JWT Token
     * @return true-已过期或无效；false-有效
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
