package com.priz.base.application.features.apikey;

import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionGroupCredentialsModel;
import com.priz.base.domain.mysql_priz_base.model.ApiKeyPermissionMappingModel;

import java.util.Set;

public interface ApiKeyPermissionService {

    ApiKeyPermissionMappingModel addDirectPermission(String apiKeyId, String permissionCode);

    void removeDirectPermission(String apiKeyId, String permissionCode);

    ApiKeyPermissionGroupCredentialsModel addPermissionGroup(String apiKeyId, String groupCode);

    void removePermissionGroup(String apiKeyId, String groupCode);

    Set<String> getEffectivePermissions(String apiKeyId);
}
