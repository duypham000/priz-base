package com.priz.base.application.features.permission;

import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;

import java.util.List;
import java.util.Set;

public interface UserPermissionService {

    UserPermissionGroupModel assignGroup(String userId, String permissionGroupCode);

    void removeGroup(String userId, String permissionGroupCode);

    List<String> getGroupCodes(String userId);

    Set<String> getPermissionCodes(String userId);

    /**
     * Trả về chuỗi bitmask permissions đã encode, dùng để nhúng vào JWT claim "perms".
     * Format: "base:article_manager=5,global:report=2"
     */
    String getEncodedPermissions(String userId);
}
