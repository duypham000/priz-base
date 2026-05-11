package com.priz.base.application.features.permission.impl;

import com.priz.base.application.features.permission.PermissionService;
import com.priz.base.application.features.permission.dto.CreatePermissionRequest;
import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionRepository;
import com.priz.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public PermissionModel create(CreatePermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Permission code đã tồn tại: " + request.getCode());
        }
        PermissionModel model = PermissionModel.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .groupCode(request.getGroupCode())
                .build();
        PermissionModel saved = permissionRepository.save(model);
        log.info("Created permission: {}", saved.getCode());
        return saved;
    }

    @Override
    public PermissionModel getByCode(String code) {
        return permissionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", code));
    }

    @Override
    public List<PermissionModel> getAll() {
        return permissionRepository.findAll();
    }

    @Override
    public void delete(String id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", "id", id);
        }
        permissionRepository.deleteById(id);
        log.info("Deleted permission id: {}", id);
    }
}
