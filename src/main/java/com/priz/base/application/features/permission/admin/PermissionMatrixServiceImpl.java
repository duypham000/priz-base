package com.priz.base.application.features.permission.admin;

import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionRepository;
import com.priz.common.security.permission.PermissionAction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionMatrixServiceImpl implements PermissionMatrixService {

    private final PermissionGroupMappingRepository mappingRepository;
    private final PermissionRepository permissionRepository;

    @Value("${spring.application.name:base}")
    private String applicationName;

    @Override
    public Map<String, List<String>> getMatrix(String groupCode) {
        List<Object[]> rows = mappingRepository.findCustomKeyMasksByGroupCodes(List.of(groupCode));
        Map<String, Integer> keyToMask = new HashMap<>();
        for (Object[] row : rows) {
            String customKey = (String) row[0];
            int mask = ((Number) row[1]).intValue();
            keyToMask.merge(customKey, mask, (a, b) -> a | b);
        }

        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : keyToMask.entrySet()) {
            List<String> actions = new ArrayList<>();
            for (PermissionAction action : PermissionAction.values()) {
                if ((entry.getValue() & action.mask()) != 0) {
                    actions.add(action.name());
                }
            }
            result.put(entry.getKey(), actions);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateMatrix(String groupCode, Map<String, List<String>> matrix) {
        for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
            String customKey = entry.getKey();
            int actionMask = entry.getValue().stream()
                    .map(s -> PermissionAction.valueOf(s.toUpperCase()))
                    .reduce(0, (acc, a) -> acc | a.mask(), (a, b) -> a | b);

            String permCode = "matrix:" + customKey + ":" + actionMask;
            if (!permissionRepository.existsByCode(permCode)) {
                permissionRepository.save(PermissionModel.builder()
                        .code(permCode)
                        .name("Matrix: " + customKey)
                        .groupCode("matrix")
                        .customKey(customKey)
                        .actionMask(actionMask)
                        .build());
            }

            if (!mappingRepository.existsByPermissionGroupCodeAndPermissionCode(groupCode, permCode)) {
                mappingRepository.save(PermissionGroupMappingModel.builder()
                        .permissionGroupCode(groupCode)
                        .permissionCode(permCode)
                        .build());
            }
        }
    }
}
