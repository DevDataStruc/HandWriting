package com.example.handwriting.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.handwriting.common.constant.CommonConstants.REDIS_KEY_BLACKLIST;

/**
 * JWT 认证过滤器：解析 Authorization 头、注入 SecurityContext。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(jwtUtil == null ? "Authorization" : "Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            String tokenType = claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class);
            if (!JwtUtil.TYPE_ACCESS.equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }
            Long userId = claims.get(JwtUtil.CLAIM_USER_ID, Long.class);
            String jti = claims.getId();
            String blacklistKey = String.format(REDIS_KEY_BLACKLIST, jti == null ? token : jti);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.debug("Token 已吊销: {}", jti);
                filterChain.doFilter(request, response);
                return;
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getOrDefault(JwtUtil.CLAIM_ROLES, List.of());
            @SuppressWarnings("unchecked")
            List<String> perms = (List<String>) claims.getOrDefault(JwtUtil.CLAIM_PERMS, List.of());
            Collection<SimpleGrantedAuthority> auths = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());

            LoginUser loginUser = new LoginUser(userId,
                    claims.get(JwtUtil.CLAIM_USERNAME, String.class),
                    roles, perms);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(loginUser, null, auths);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            log.warn("JWT 解析失败: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
