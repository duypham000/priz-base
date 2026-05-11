package com.priz.base.application.features.permission;

import com.priz.base.application.features.permission.dto.CreatePermissionGroupRequest;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;

import java.util.List;

public interface PermissionGroupService {

    PermissionGroupModel create(CreatePermissionGroupRequest request);

    PermissionGroupModel getByCode(String code);

    List<PermissionGroupModel> getAll();

    PermissionGroupMappingModel addPermission(String groupCode, String permissionCode);

    void removePermission(String groupCode, String permissionCode);

    List<String> getPermissionCodes(String groupCode);

    void delete(String id);
}
