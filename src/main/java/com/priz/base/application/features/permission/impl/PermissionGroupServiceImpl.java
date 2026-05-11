package com.priz.base.application.features.permission.impl;

import com.priz.base.application.features.permission.PermissionGroupService;
import com.priz.base.application.features.permission.dto.CreatePermissionGroupRequest;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupMappingModel;
import com.priz.base.domain.mysql_priz_base.model.PermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionRepository;
import com.priz.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private final PermissionGroupRepository groupRepository;
    private final PermissionGroupMappingRepository mappingRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public PermissionGroupModel create(CreatePermissionGroupRequest request) {
        if (groupRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Group code đã tồn tại: " + request.getCode());
        }
        PermissionGroupModel model = PermissionGroupModel.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();
        PermissionGroupModel saved = groupRepository.save(model);
        log.info("Created permission group: {}", saved.getCode());
        return saved;
    }

    @Override
    public PermissionGroupModel getByCode(String code) {
        return groupRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionGroup", "code", code));
    }

    @Override
    public List<PermissionGroupModel> getAll() {
        return groupRepository.findAll();
    }

    @Override
    public PermissionGroupMappingModel addPermission(String groupCode, String permissionCode) {
        if (!groupRepository.existsByCode(groupCode)) {
            throw new ResourceNotFoundException("PermissionGroup", "code", groupCode);
        }
        if (!permissionRepository.existsByCode(permissionCode)) {
            throw new ResourceNotFoundException("Permission", "code", permissionCode);
        }
        if (mappingRepository.existsByPermissionGroupCodeAndPermissionCode(groupCode, permissionCode)) {
            throw new IllegalArgumentException("Mapping đã tồn tại: " + groupCode + " -> " + permissionCode);
        }
        PermissionGroupMappingModel mapping = PermissionGroupMappingModel.builder()
                .permissionGroupCode(groupCode)
                .permissionCode(permissionCode)
                .build();
        return mappingRepository.save(mapping);
    }

    @Override
    public void removePermission(String groupCode, String permissionCode) {
        mappingRepository.deleteByPermissionGroupCodeAndPermissionCode(groupCode, permissionCode);
    }

    @Override
    public List<String> getPermissionCodes(String groupCode) {
        return mappingRepository.findByPermissionGroupCode(groupCode)
                .stream()
                .map(PermissionGroupMappingModel::getPermissionCode)
                .toList();
    }

    @Override
    public void delete(String id) {
        PermissionGroupModel group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionGroup", "id", id));
        mappingRepository.deleteByPermissionGroupCode(group.getCode());
        groupRepository.deleteById(id);
        log.info("Deleted permission group id: {}", id);
    }
}
