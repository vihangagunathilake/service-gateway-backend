package com.flex.common_module.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flex.common_module.http.ReturnResponse;
import com.flex.common_module.security.constants.SecurityConstants;
import com.flex.common_module.security.impls.repositories.ExpiredTokenRepository;
import com.flex.common_module.security.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Slf4j
@Component
@SuppressWarnings("Duplicates")
public class JwtAuthenticationFilter extends OncePerRequestFilter {private static final Set<String> EXCLUDED_PATHS = Set.of(SecurityConstants.EXCLUDED_PATHS);

    private final UserDetailsService userDetailsService;
    private final ExpiredTokenRepository expiredTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(UserDetailsService userDetailsService,
                                   ExpiredTokenRepository expiredTokenRepository) {
        this.userDetailsService = userDetailsService;
        this.expiredTokenRepository = expiredTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ✅ WebSocket bypass
        if (path.startsWith("/ws") || path.startsWith("/service-gateway/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (EXCLUDED_PATHS.contains(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Invalid or missing Authorization header");
            rejectRequest(response, "Invalid or missing Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (expiredTokenRepository.existsById(token)) {
            log.error("Token is expired");
            rejectRequest(response, "Token is expired");
            return;
        }

        try {
            Claims claims = JwtUtil.extractTokenBody(token);
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            log.error("JWT validation failed", e);
            rejectRequest(response, "JWT validation failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        ReturnResponse.FORBIDDEN(objectMapper, response);
        log.warn("Request rejected: {}", message);
    }

}
