package com.priz.base.infrastructure.config;

import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.repository.UserPermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_GROUP_CODE = "BASE_ADMIN";

    private final UserRepository userRepository;
    private final UserPermissionGroupRepository userPermissionGroupRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        UserModel admin = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (admin == null) {
            admin = UserModel.builder()
                    .username("admin")
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode("admin"))
                    .fullName("Administrator")
                    .role(UserModel.Role.ADMIN)
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);
            log.info("Default admin user created: {} / admin", ADMIN_EMAIL);
        } else {
            log.info("Admin user already exists: {}", ADMIN_EMAIL);
        }

        if (!userPermissionGroupRepository.existsByUserIdAndPermissionGroupCode(
                admin.getId(), ADMIN_GROUP_CODE)) {
            userPermissionGroupRepository.save(UserPermissionGroupModel.builder()
                    .userId(admin.getId())
                    .permissionGroupCode(ADMIN_GROUP_CODE)
                    .build());
            log.info("Assigned admin user to {} group", ADMIN_GROUP_CODE);
        }
    }
}
