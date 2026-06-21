package com.example.handwriting.auth.service;

import com.example.handwriting.admin.entity.Role;
import com.example.handwriting.admin.entity.UserRole;
import com.example.handwriting.admin.repository.RoleRepository;
import com.example.handwriting.admin.repository.UserRoleRepository;
import com.example.handwriting.auth.dto.LoginDTO;
import com.example.handwriting.auth.dto.LoginVO;
import com.example.handwriting.auth.dto.RegisterDTO;
import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.util.WebUtils;
import com.example.handwriting.security.JwtUtil;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.handwriting.common.constant.CommonConstants.REDIS_KEY_REFRESH;

/**
 * 鉴权服务：登录、注册、刷新、注销
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaService captchaService;

    @Value("${app.jwt.refresh-token-ttl-seconds:604800}")
    private long refreshTtl;

    @Transactional
    public LoginVO login(LoginDTO dto) {
        // 校验图形验证码（内存存储）
        if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BizException(ErrorCode.CAPTCHA_INVALID);
        }

        User u = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.USER_PASSWORD_INVALID));
        if (u.getStatus() != null && u.getStatus() == CommonConstants.USER_STATUS_DISABLED) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), u.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_INVALID);
        }

        // 加载角色
        List<String> roles = userRoleRepository.findByUserId(u.getId()).stream()
                .map(UserRole::getRoleId)
                .map(rid -> roleRepository.findById(rid).map(Role::getCode).orElse(""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            roles = List.of(CommonConstants.ROLE_USER);
        }
        List<String> perms = permissionsOf(roles);

        String access = jwtUtil.generateAccessToken(u.getId(), u.getUsername(), roles, perms);
        String refresh = jwtUtil.generateRefreshToken(u.getId(), u.getUsername());
        redisTemplate.opsForValue().set(
                String.format(REDIS_KEY_REFRESH, refresh),
                String.valueOf(u.getId()),
                Duration.ofSeconds(refreshTtl));

        // 更新登录态
        u.setLastLoginTime(LocalDateTime.now());
        u.setLastLoginIp(WebUtils.clientIp());
        userRepository.save(u);

        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTtlSeconds())
                .user(LoginVO.UserInfo.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .avatar(u.getAvatar())
                        .roles(roles)
                        .permissions(perms)
                        .build())
                .build();
    }

    @Transactional
    public LoginVO register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setStatus(CommonConstants.USER_STATUS_NORMAL);
        userRepository.save(u);

        // 默认赋予 USER 角色
        Role userRole = roleRepository.findByCode(CommonConstants.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(CommonConstants.ROLE_USER, "普通用户")));
        userRoleRepository.save(new UserRole(u.getId(), userRole.getId()));

        // 注册时不再要求图形验证码，直接颁发 token
        return doLoginWithoutCaptcha(u, List.of(CommonConstants.ROLE_USER), permissionsOf(List.of(CommonConstants.ROLE_USER)));
    }

    private LoginVO doLoginWithoutCaptcha(User u, List<String> roles, List<String> perms) {
        String access = jwtUtil.generateAccessToken(u.getId(), u.getUsername(), roles, perms);
        String refresh = jwtUtil.generateRefreshToken(u.getId(), u.getUsername());
        redisTemplate.opsForValue().set(
                String.format(REDIS_KEY_REFRESH, refresh),
                String.valueOf(u.getId()),
                Duration.ofSeconds(refreshTtl));
        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTtlSeconds())
                .user(LoginVO.UserInfo.builder()
                        .id(u.getId()).username(u.getUsername()).nickname(u.getNickname())
                        .avatar(u.getAvatar()).roles(roles).permissions(perms).build())
                .build();
    }

    public LoginVO refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        try {
            var claims = jwtUtil.parse(refreshToken);
            if (!JwtUtil.TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
                throw new BizException(ErrorCode.TOKEN_INVALID);
            }
        } catch (Exception ex) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        Object cached = redisTemplate.opsForValue().get(String.format(REDIS_KEY_REFRESH, refreshToken));
        if (cached == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(cached.toString());
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_EXISTS));
        List<String> roles = userRoleRepository.findByUserId(u.getId()).stream()
                .map(UserRole::getRoleId)
                .map(rid -> roleRepository.findById(rid).map(Role::getCode).orElse(""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (roles.isEmpty()) roles = List.of(CommonConstants.ROLE_USER);
        List<String> perms = permissionsOf(roles);
        return doLoginWithoutCaptcha(u, roles, perms);
    }

    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null) {
            redisTemplate.delete(String.format(REDIS_KEY_REFRESH, refreshToken));
        }
        if (accessToken != null) {
            String jti = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    String.format(CommonConstants.REDIS_KEY_BLACKLIST, jti),
                    "1",
                    Duration.ofSeconds(jwtUtil.getAccessTtlSeconds()));
        }
        log.info("User logged out, jti={}", accessToken);
    }

    /** 简单的角色→权限点映射；生产可从 DB 加载 */
    private List<String> permissionsOf(List<String> roles) {
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        java.util.Set<String> perms = new java.util.HashSet<>();
        perms.add(CommonConstants.PERM_SAMPLE_UPLOAD);
        if (roles.contains(CommonConstants.ROLE_AUDITOR) || roles.contains(CommonConstants.ROLE_ADMIN)) {
            perms.add(CommonConstants.PERM_AUDIT_APPROVE);
            perms.add(CommonConstants.PERM_AUDIT_REJECT);
        }
        if (roles.contains(CommonConstants.ROLE_ADMIN)) {
            perms.add(CommonConstants.PERM_ADMIN_USER);
            perms.add(CommonConstants.PERM_ADMIN_ROLE);
        }
        return new java.util.ArrayList<>(perms);
    }
}
