package com.priz.base.application.features.apikey.impl;

import com.priz.base.application.features.apikey.ApiKeyPermissionService;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupCredentialsModel;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionMappingModel;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyCredentialsRepository;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyPermissionGroupCredentialsRepository;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyPermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyPermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyPermissionMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.ApiKeyPermissionRepository;
import com.priz.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiKeyPermissionServiceImpl implements ApiKeyPermissionService {

    private final ApiKeyCredentialsRepository credentialsRepository;
    private final ApiKeyPermissionRepository permissionRepository;
    private final ApiKeyPermissionMappingRepository directMappingRepository;
    private final ApiKeyPermissionGroupRepository groupRepository;
    private final ApiKeyPermissionGroupCredentialsRepository groupCredentialsRepository;
    private final ApiKeyPermissionGroupMappingRepository groupMappingRepository;

    @Override
    @Transactional
    public ApiKeyPermissionMappingModel addDirectPermission(String apiKeyId, String permissionCode) {
        requireCredentials(apiKeyId);
        if (!permissionRepository.existsByCode(permissionCode)) {
            throw new ResourceNotFoundException("ApiKeyPermission", "code", permissionCode);
        }
        if (directMappingRepository.existsByApiKeyCredentialsIdAndPermissionCode(apiKeyId, permissionCode)) {
            throw new IllegalArgumentException("Permission đã được gán: " + permissionCode);
        }
        return directMappingRepository.save(ApiKeyPermissionMappingModel.builder()
                .apiKeyCredentialsId(apiKeyId)
                .permissionCode(permissionCode)
                .build());
    }

    @Override
    @Transactional
    public void removeDirectPermission(String apiKeyId, String permissionCode) {
        directMappingRepository.deleteByApiKeyCredentialsIdAndPermissionCode(apiKeyId, permissionCode);
    }

    @Override
    @Transactional
    public ApiKeyPermissionGroupCredentialsModel addPermissionGroup(String apiKeyId, String groupCode) {
        requireCredentials(apiKeyId);
        if (!groupRepository.existsByCode(groupCode)) {
            throw new ResourceNotFoundException("ApiKeyPermissionGroup", "code", groupCode);
        }
        if (groupCredentialsRepository.existsByApiKeyCredentialsIdAndPermissionGroupCode(apiKeyId, groupCode)) {
            throw new IllegalArgumentException("Group đã được gán: " + groupCode);
        }
        return groupCredentialsRepository.save(ApiKeyPermissionGroupCredentialsModel.builder()
                .apiKeyCredentialsId(apiKeyId)
                .permissionGroupCode(groupCode)
                .build());
    }

    @Override
    @Transactional
    public void removePermissionGroup(String apiKeyId, String groupCode) {
        groupCredentialsRepository.deleteByApiKeyCredentialsIdAndPermissionGroupCode(apiKeyId, groupCode);
    }

    @Override
    public Set<String> getEffectivePermissions(String apiKeyId) {
        Set<String> permissions = new HashSet<>();

        List<String> direct = directMappingRepository.findPermissionCodesByApiKeyCredentialsId(apiKeyId);
        permissions.addAll(direct);

        List<String> groupCodes = groupCredentialsRepository.findGroupCodesByApiKeyCredentialsId(apiKeyId);
        if (!groupCodes.isEmpty()) {
            permissions.addAll(groupMappingRepository.findDistinctPermissionCodesByGroupCodes(groupCodes));
        }

        return permissions;
    }

    private void requireCredentials(String apiKeyId) {
        if (!credentialsRepository.existsById(apiKeyId)) {
            throw new ResourceNotFoundException("ApiKeyCredentials", "id", apiKeyId);
        }
    }
}
