package com.priz.base.application.features.permission.admin;

import java.util.List;
import java.util.Map;

public interface PermissionMatrixService {

    /**
     * Trả về bitmask matrix cho một group code.
     * Format: { "base:article_manager": ["CREATE", "READ"] }
     */
    Map<String, List<String>> getMatrix(String groupCode);

    /**
     * Upsert bitmask mappings cho một group code.
     * Body: { "base:article_manager": ["CREATE", "READ"] }
     */
    void updateMatrix(String groupCode, Map<String, List<String>> matrix);
}
