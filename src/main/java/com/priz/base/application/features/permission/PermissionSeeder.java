package com.priz.base.application.features.permission;

import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionSeeder implements ApplicationRunner {

    private static final String ADMIN_GROUP_CODE = "BASE_ADMIN";

    // format: [code, name, customKey, actionMask]
    private static final List<Object[]> BASE_PERMISSIONS = List.of(
            new Object[]{"manage:base:permission",         "Manage Permissions",         "base:permission",         31},
            new Object[]{"manage:base:permission_group",   "Manage Permission Groups",   "base:permission_group",   31},
            new Object[]{"manage:base:user_permission",    "Manage User Permissions",    "base:user_permission",    31},
            new Object[]{"manage:base:api_key",            "Manage API Keys",            "base:api_key",            31},
            new Object[]{"manage:base:api_key_permission", "Manage API Key Permissions", "base:api_key_permission", 31},
            new Object[]{"manage:global:permission_admin", "Manage Permission Matrix",   "global:permission_admin", 31}
    );

    private final PermissionRepository permissionRepository;
    private final PermissionGroupRepository groupRepository;
    private final PermissionGroupMappingRepository mappingRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedAdminGroup();
        log.info("Permission seed completed");
    }

    private void seedPermissions() {
        for (Object[] entry : BASE_PERMISSIONS) {
            String code = (String) entry[0];
            if (!permissionRepository.existsByCode(code)) {
                permissionRepository.save(PermissionModel.builder()
                        .code(code)
                        .name((String) entry[1])
                        .groupCode("base")
                        .customKey((String) entry[2])
                        .actionMask((Integer) entry[3])
                        .build());
                log.info("Seeded permission: {}", code);
            }
        }
    }

    private void seedAdminGroup() {
        if (!groupRepository.existsByCode(ADMIN_GROUP_CODE)) {
            groupRepository.save(PermissionGroupModel.builder()
                    .code(ADMIN_GROUP_CODE)
                    .name("Base Admin")
                    .description("Full access to base admin operations")
                    .build());
            log.info("Seeded permission group: {}", ADMIN_GROUP_CODE);
        }

        for (Object[] entry : BASE_PERMISSIONS) {
            String permCode = (String) entry[0];
            if (!mappingRepository.existsByPermissionGroupCodeAndPermissionCode(
                    ADMIN_GROUP_CODE, permCode)) {
                mappingRepository.save(PermissionGroupMappingModel.builder()
                        .permissionGroupCode(ADMIN_GROUP_CODE)
                        .permissionCode(permCode)
                        .build());
            }
        }
    }
}
