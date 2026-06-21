package com.example.handwriting.config;

import com.example.handwriting.admin.entity.Role;
import com.example.handwriting.admin.entity.UserRole;
import com.example.handwriting.admin.repository.RoleRepository;
import com.example.handwriting.admin.repository.UserRoleRepository;
import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开发环境默认管理员种子：
 * 用户名 admin，密码 admin123，角色 ADMIN/USER。
 * 仅在 admin 用户不存在时创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("Admin user already exists, skip seeding.");
            return;
        }

        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setNickname("系统管理员");
        admin.setStatus(CommonConstants.USER_STATUS_NORMAL);
        userRepository.save(admin);

        Role adminRole = roleRepository.findByCode(CommonConstants.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
        Role userRole = roleRepository.findByCode(CommonConstants.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found"));

        userRoleRepository.save(new UserRole(admin.getId(), adminRole.getId()));
        userRoleRepository.save(new UserRole(admin.getId(), userRole.getId()));

        log.info("Seeded default admin account: {}/{} (id={})", ADMIN_USERNAME, ADMIN_PASSWORD, admin.getId());
    }
}
