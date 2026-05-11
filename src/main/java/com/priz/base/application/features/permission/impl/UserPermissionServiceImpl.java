package com.priz.base.application.features.permission.impl;

import com.priz.base.application.features.permission.UserPermissionService;
import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserPermissionGroupRepository;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.common.security.permission.PermissionCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserPermissionGroupRepository userGroupRepository;
    private final PermissionGroupRepository groupRepository;
    private final PermissionGroupMappingRepository mappingRepository;

    @Override
    public UserPermissionGroupModel assignGroup(String userId, String permissionGroupCode) {
        if (!groupRepository.existsByCode(permissionGroupCode)) {
            throw new ResourceNotFoundException("PermissionGroup", "code", permissionGroupCode);
        }
        if (userGroupRepository.existsByUserIdAndPermissionGroupCode(userId, permissionGroupCode)) {
            throw new IllegalArgumentException(
                    "User " + userId + " đã có group: " + permissionGroupCode);
        }
        UserPermissionGroupModel model = UserPermissionGroupModel.builder()
                .userId(userId)
                .permissionGroupCode(permissionGroupCode)
                .build();
        return userGroupRepository.save(model);
    }

    @Override
    public void removeGroup(String userId, String permissionGroupCode) {
        userGroupRepository.deleteByUserIdAndPermissionGroupCode(userId, permissionGroupCode);
    }

    @Override
    public List<String> getGroupCodes(String userId) {
        return userGroupRepository.findGroupCodesByUserId(userId);
    }

    @Override
    public Set<String> getPermissionCodes(String userId) {
        List<String> groupCodes = userGroupRepository.findGroupCodesByUserId(userId);
        if (groupCodes.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                mappingRepository.findDistinctPermissionCodesByGroupCodes(groupCodes));
    }

    @Override
    public String getEncodedPermissions(String userId) {
        List<String> groupCodes = userGroupRepository.findGroupCodesByUserId(userId);
        if (groupCodes.isEmpty()) {
            return "";
        }
        List<Object[]> rows = mappingRepository.findCustomKeyMasksByGroupCodes(groupCodes);
        if (rows.isEmpty()) {
            return "";
        }
        Map<String, Integer> keyToMask = new HashMap<>();
        for (Object[] row : rows) {
            String customKey = (String) row[0];
            int actionMask = ((Number) row[1]).intValue();
            keyToMask.merge(customKey, actionMask, (a, b) -> a | b);
        }
        return PermissionCodec.encode(keyToMask);
    }
}
