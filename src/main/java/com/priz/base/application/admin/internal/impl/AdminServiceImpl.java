package com.priz.base.application.admin.internal.impl;

import com.priz.base.application.admin.internal.AdminService;
import com.priz.base.application.admin.internal.converter.AdminEntityConverter;
import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.base.application.admin.internal.introspector.EntitySchemaIntrospector;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistry;
import com.priz.base.application.admin.internal.specification.GenericSpecificationBuilder;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.base.common.model.BaseModel;
import com.priz.interfaces.admin.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminEntityRegistry registry;
    private final EntitySchemaIntrospector introspector;
    private final GenericSpecificationBuilder specBuilder;

    @Override
    public List<String> getRegisteredTables() {
        return registry.getRegisteredTableNames();
    }

    @Override
    public TableSchemaResponse getTableSchema(String tableName) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        return introspector.introspect(reg);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRecords(String tableName, AdminFilterRequest request) {
        AdminEntityRegistration reg = requireRegistration(tableName);

        @SuppressWarnings("unchecked")
        JpaSpecificationExecutor<BaseModel> specExec = (JpaSpecificationExecutor<BaseModel>) reg.specExecutor();

        Specification<BaseModel> spec = specBuilder.build(request.getFilters(), reg);

        Page<BaseModel> page = specExec.findAll(spec, request.getPagination().toPageable());
        Page<Map<String, Object>> mapped = page.map(entity -> AdminEntityConverter.toMap(entity, reg.hiddenFields()));

        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRecord(String tableName, String id) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        BaseModel entity = findEntityById(reg, id);
        return AdminEntityConverter.toMap(entity, reg.hiddenFields());
    }

    @Override
    @Transactional
    public Map<String, Object> createRecord(String tableName, Map<String, Object> data) {
        AdminEntityRegistration reg = requireRegistration(tableName);

        BaseModel entity;
        try {
            entity = reg.entityClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo instance cho entity: " + reg.entityClass().getSimpleName(), e);
        }

        AdminEntityConverter.applyUpdate(entity, data, reg);

        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        BaseModel saved = repo.save(entity);

        return AdminEntityConverter.toMap(saved, reg.hiddenFields());
    }

    @Override
    @Transactional
    public Map<String, Object> updateRecord(String tableName, String id, Map<String, Object> data) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        BaseModel entity = findEntityById(reg, id);

        AdminEntityConverter.applyUpdate(entity, data, reg);

        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        BaseModel saved = repo.save(entity);

        return AdminEntityConverter.toMap(saved, reg.hiddenFields());
    }

    @Override
    @Transactional
    public void deleteRecord(String tableName, String id) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        // Kiểm tra tồn tại trước khi xóa
        findEntityById(reg, id);

        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(String tableName, List<String> ids) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        repo.deleteAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportTable(String tableName) {
        AdminEntityRegistration reg = requireRegistration(tableName);
        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        List<BaseModel> all = repo.findAll();
        
        List<Map<String, Object>> mapped = all.stream()
                .map(entity -> AdminEntityConverter.toMap(entity, reg.hiddenFields()))
                .toList();

        if (mapped.isEmpty()) return "";

        Set<String> headers = mapped.get(0).keySet();
        StringBuilder csv = new StringBuilder();
        
        // Header row
        csv.append(String.join(",", headers)).append("\n");
        
        // Data rows
        for (Map<String, Object> row : mapped) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object val = row.get(header);
                String strVal = (val == null) ? "" : val.toString();
                // Escape double quotes and wrap in quotes
                values.add("\"" + strVal.replace("\"", "\"\"") + "\"");
            }
            csv.append(String.join(",", values)).append("\n");
        }
        
        return csv.toString();
    }

    private AdminEntityRegistration requireRegistration(String tableName) {
        return registry.getRegistration(tableName)
                .orElseThrow(() -> new ResourceNotFoundException("Table", "name", tableName));
    }

    private BaseModel findEntityById(AdminEntityRegistration reg, String id) {
        @SuppressWarnings("unchecked")
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        Optional<BaseModel> entity = repo.findById(id);
        return entity.orElseThrow(() ->
                new ResourceNotFoundException(reg.entityClass().getSimpleName(), "id", id));
    }
}
