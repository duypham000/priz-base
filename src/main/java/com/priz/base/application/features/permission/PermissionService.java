package com.priz.base.application.features.permission;

import com.priz.base.application.features.permission.dto.CreatePermissionRequest;
import com.priz.base.domain.mysql_priz_base.model.PermissionModel;

import java.util.List;

public interface PermissionService {

    PermissionModel create(CreatePermissionRequest request);

    PermissionModel getByCode(String code);

    List<PermissionModel> getAll();

    void delete(String id);
}
