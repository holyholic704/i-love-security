package com.example.security.util;

import cn.hutool.core.util.StrUtil;
import com.example.security.bean.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodecs;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    /**
     * 加密密钥
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * token有效期
     */
    @Value("${jwt.expired}")
    private Long expired;

    /**
     * 用户id
     */
    private final String CLAIMS_USER_ID = "user_id";

    /**
     * 生成token
     *
     * @param user 用户信息
     * @return token
     */
    public String generate(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIMS_USER_ID, user.getId());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setExpiration(this.generateExpiration(expired))
                .setIssuedAt(new Date())
                .compressWith(CompressionCodecs.DEFLATE)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 获取到期时间
     *
     * @param expiredTime 有效时间
     * @return 到期时间
     */
    private Date generateExpiration(long expiredTime) {
        return new Date(System.currentTimeMillis() + expiredTime * 1000);
    }

    /**
     * 验证token
     *
     * @param token token
     * @param user  用户信息
     * @return 是否通过认证
     */
    public boolean check(String token, User user) {
        return StrUtil.isNotEmpty(token) && user != null && this.checkUser(token, user);
    }

    /**
     * 验证token
     *
     * @param token token
     * @param user  用户信息
     * @return 是否通过认证
     */
    private boolean checkUser(String token, User user) {
        // 获取token中的信息
        Claims claims = this.getClaimsFromToken(token);
        // 校验
        return String.valueOf(user.getId()).equals(String.valueOf(claims.get(CLAIMS_USER_ID)))
                && user.getUsername().equals(claims.getSubject())
                && !isTokenExpired(token);
    }

    /**
     * 获取token中的信息
     *
     * @param token token
     * @return token中的信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(this.secret).parseClaimsJws(token).getBody();
    }

    /**
     * 判断token是否在有效期内
     *
     * @param token token
     * @return 是否在有效期内
     */
    public boolean isTokenExpired(String token) {
        return this.getExpirationFromToken(token).before(new Date());
    }

    /**
     * 获取token中的到期时间
     *
     * @param token token
     * @return 到期时间
     */
    public Date getExpirationFromToken(String token) {
        return this.getClaimsFromToken(token).getExpiration();
    }

    /**
     * 获取token中的用户名
     *
     * @param token token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return this.getClaimsFromToken(token).getSubject();
    }

    /**
     * 获取token中的用户ID
     *
     * @param token token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(String.valueOf(this.getClaimsFromToken(token).get(CLAIMS_USER_ID)));
    }

    /**
     * 获取token中的用户信息
     *
     * @param token token
     * @return 用户信息
     */
    public User getUserFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        long userId = Long.parseLong(String.valueOf(claims.get(CLAIMS_USER_ID)));
        return new User(userId, claims.getSubject());
    }
}
