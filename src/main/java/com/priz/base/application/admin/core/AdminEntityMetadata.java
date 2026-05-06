package com.priz.base.application.admin.core;

import com.priz.base.common.model.BaseModel;

import java.util.Set;

public record AdminEntityMetadata(
        String resourceName,
        String displayName,
        Class<? extends BaseModel> entityClass,
        Set<String> hiddenFields,
        Set<String> readOnlyFields) {

    public static final Set<String> BASE_READ_ONLY =
            Set.of("id", "createdAt", "updatedAt", "createdBy", "updatedBy");
}
