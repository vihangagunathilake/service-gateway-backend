package com.flex.common_module.security.utils;

import com.flex.common_module.exceptions.UserTokenException;
import com.flex.common_module.security.http.response.UserClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import static com.flex.common_module.security.constants.SecurityConstants.SECRET_KEY;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Slf4j
@Component
@SuppressWarnings("Duplicates")
public class JwtUtil {
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

// when generateToken
//   Map<String, Object> claims = new HashMap<>();
//   claims.put("userId", user.getId());
//   claims.put("companyId", user.getCompany().getId());
//   claims.put("role", "ROLE_" + user.getRole());

    // 🛠️ Generate token with username and userId
    public String generateToken(Map<String, Object> claimMap, String username) {

        Claims claims = Jwts.claims().setSubject(username);

        if (claimMap != null) {
            for (String key : claimMap.keySet()) {
                claims.put(key, claimMap.get(key));
            }
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date()) // 🕒 Now
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // ⏳ 10 min
                .signWith(key) // 🔐 Sign with key
                .compact(); // 📦 Build token
    }

    // 🛠️ Refresh token
    public String refreshToken(Map<String, Object> claimMap, String username) {

        Claims claims = Jwts.claims().setSubject(username);

        if (claimMap != null) {
            for (String key : claimMap.keySet()) {
                claims.put(key, claimMap.get(key));
            }
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date()) // 🕒 Now
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // ⏳ 1 day
                .signWith(key) // 🔐 Sign with key
                .compact(); // 📦 Build token
    }

    public static Claims extractTokenBody(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static UserClaims getClaimsFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null) {
            log.error("no auth header found in {}" ,request.getRequestURI());
            throw new UserTokenException("Something wrong with your token");
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.error("no bearer with JWT in {}" ,request.getRequestURI());
            throw new UserTokenException("Something missing in your token");
        }

        String token = authHeader.substring(7);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return UserClaims.builder()
                .email(claims.getSubject())
                .userId(claims.get("user", Integer.class))
                .center(claims.get("center", Integer.class))
                .provider(claims.get("provider", String.class))
                .type(claims.get("type", Integer.class))
                .build();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (AuthorizationDeniedException e) {
            System.out.println("Invalid Token");
        }
        return false;
    }
}
