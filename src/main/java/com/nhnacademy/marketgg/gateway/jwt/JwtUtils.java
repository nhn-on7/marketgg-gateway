package com.nhnacademy.marketgg.gateway.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.security.Key;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * JWT 다루는 Util 클래스 입니다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtils {

    public static final String AUTHORITIES = "AUTHORITIES";

    /**
     * JWT 서명에 필요한 키를 생성합니다.
     *
     * @param jwtSecretUrl - JWT Secret 을 요청하는 URL 입니다.
     * @return JWT 서명에 쓰이는 Key 를 반환합니다.
     */
    public static Key getKey(String jwtSecretUrl) {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(getJwtSecret(jwtSecretUrl)));
    }

    private static String getJwtSecret(String jwtSecretUrl) {
        Map<String, Map<String, String>> block = WebClient.create()
                                                          .get()
                                                          .uri(jwtSecretUrl)
                                                          .retrieve()
                                                          .bodyToMono(Map.class)
                                                          .timeout(Duration.ofSeconds(3))
                                                          .block();

        return Optional.ofNullable(block)
                       .orElseThrow(IllegalArgumentException::new)
                       .get("body")
                       .get("secret");
    }

    /**
     * 토큰을 파싱하여 사용 가능한 토큰인지 확인합니다.
     *
     * @param token - 사용자의 JWT 입니다.
     * @param key - 토큰 파싱에 필요한 Key 입니다.
     * @return 사용가능한 JWT 를 반환합니다.
     */
    public static String parseToken(String token, Key key) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

            return token;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.", e);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.", e);
        }
        return null;
    }

    /**
     * 토큰을 이용하여 사용자의 권한 정보를 얻습니다.
     *
     * @param token - JWT.
     * @param key - JWT 파싱에 필요한 Key 입니다.
     * @return 사용자의 권한을 반환합니다.
     */
    public static String getRoles(String token, Key key) {
        String roles = "";
        ObjectMapper mapper = new ObjectMapper();

        try {
            roles = mapper.writeValueAsString(getClaims(token, key).get(AUTHORITIES));
        } catch (IOException e) {
            log.error("JSON ERROR", e);
        }

        return roles;
    }

    public static String getUuid(String token, Key key) {
        return getClaims(token, key).getSubject();
    }

    private static Claims getClaims(String token, Key key) {
        return Jwts.parserBuilder()
                   .setSigningKey(key)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }
}
