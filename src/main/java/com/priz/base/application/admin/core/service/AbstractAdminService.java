package com.priz.base.application.admin.core.service;

import com.priz.base.application.admin.core.AdminEntityMetadata;
import com.priz.base.application.admin.core.introspector.EntitySchemaIntrospector;
import com.priz.base.application.admin.core.registry.AdminResourceRegistry;
import com.priz.base.application.admin.core.specification.GenericSpecificationBuilder;
import com.priz.base.common.model.BaseModel;
import com.priz.common.admin.annotation.AdminHidden;
import com.priz.common.admin.annotation.AdminManaged;
import com.priz.common.admin.handler.RelationshipHandlerService;
import com.priz.common.exception.BusinessException;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAdminService<E extends BaseModel, ID> {

    protected final JpaRepository<E, ID> repository;
    @SuppressWarnings("unchecked")
    private final JpaSpecificationExecutor<E> specExecutor;
    protected final AdminEntityMetadata metadata;
    private final EntitySchemaIntrospector introspector;
    private final GenericSpecificationBuilder specBuilder;
    private final RelationshipHandlerService handlerService;
    private final AdminResourceRegistry registry;

    @SuppressWarnings("unchecked")
    protected AbstractAdminService(JpaRepository<E, ID> repository,
                                    Class<E> entityClass,
                                    String resourceName,
                                    EntitySchemaIntrospector introspector,
                                    GenericSpecificationBuilder specBuilder,
                                    RelationshipHandlerService handlerService,
                                    AdminResourceRegistry registry) {
        this.repository = repository;
        this.specExecutor = (JpaSpecificationExecutor<E>) repository;
        this.introspector = introspector;
        this.specBuilder = specBuilder;
        this.handlerService = handlerService;
        this.registry = registry;
        this.metadata = buildMetadata(entityClass, resourceName);
    }

    @PostConstruct
    public void selfRegister() {
        registry.register(metadata.resourceName(), this);
    }

    // ---- Schema ----

    public TableSchemaResponse getSchema() {
        return introspector.introspect(metadata);
    }

    // ---- CRUD ----

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRecords(AdminFilterRequest request, List<String> includes) {
        @SuppressWarnings("unchecked")
        Specification<BaseModel> spec = (Specification<BaseModel>) specBuilder.build(request.getFilters(), metadata);

        @SuppressWarnings("unchecked")
        JpaSpecificationExecutor<BaseModel> exec = (JpaSpecificationExecutor<BaseModel>) specExecutor;
        Page<BaseModel> page = exec.findAll(spec, request.getPagination().toPageable());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = page.getContent().stream()
                .map(entity -> toMap((E) entity))
                .toList();

        handlerService.populate(rows, includes);

        Page<Map<String, Object>> mapped = new org.springframework.data.domain.PageImpl<>(
                rows, page.getPageable(), page.getTotalElements());
        return PageResponse.of(mapped);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecord(ID id, List<String> includes) {
        E entity = findById(id);
        Map<String, Object> row = toMap(entity);
        if (includes != null && !includes.isEmpty()) {
            handlerService.populate(List.of(row), includes);
        }
        return row;
    }

    @Transactional
    public Map<String, Object> createRecord(Map<String, Object> data) {
        E entity;
        try {
            @SuppressWarnings("unchecked")
            E created = (E) metadata.entityClass().getDeclaredConstructor().newInstance();
            entity = created;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo instance cho entity: " + metadata.entityClass().getSimpleName(), e);
        }
        beforeCreate(entity);
        applyUpdate(entity, data);
        E saved = repository.save(entity);
        afterCreate(saved);
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> updateRecord(ID id, Map<String, Object> data) {
        E entity = findById(id);
        beforeUpdate(entity);
        applyUpdate(entity, data);
        E saved = repository.save(entity);
        afterUpdate(saved);
        return toMap(saved);
    }

    @Transactional
    public void deleteRecord(ID id) {
        E entity = findById(id);
        beforeDelete(entity);
        repository.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<ID> ids) {
        repository.deleteAllById(ids);
    }

    @Transactional(readOnly = true)
    public String exportTable() {
        List<Map<String, Object>> allRows = new ArrayList<>();
        int pageSize = 1000;
        int pageNum = 0;
        Page<E> page;
        do {
            Pageable pageable = PageRequest.of(pageNum++, pageSize, Sort.by("createdAt").descending());
            @SuppressWarnings("unchecked")
            JpaSpecificationExecutor<E> exec = (JpaSpecificationExecutor<E>) specExecutor;
            page = exec.findAll(null, pageable);
            page.getContent().forEach(entity -> allRows.add(toMap(entity)));
        } while (!page.isLast());

        if (allRows.isEmpty()) return "";

        Set<String> headers = allRows.get(0).keySet();
        headers.remove("refs");

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : allRows) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object val = row.get(header);
                String strVal = (val == null) ? "" : val.toString();
                values.add("\"" + strVal.replace("\"", "\"\"") + "\"");
            }
            csv.append(String.join(",", values)).append("\n");
        }
        return csv.toString();
    }

    // ---- Hooks (override in subclass) ----

    protected void beforeCreate(E entity) {}
    protected void afterCreate(E entity) {}
    protected void beforeUpdate(E entity) {}
    protected void afterUpdate(E entity) {}
    protected void beforeDelete(E entity) {}

    // ---- Conversion ----

    public Map<String, Object> toMap(E entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (metadata.hiddenFields().contains(field.getName())) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.isSynthetic()) continue;
                field.setAccessible(true);
                try {
                    Object value = field.get(entity);
                    if (value instanceof Enum<?> e) value = e.name();
                    result.put(field.getName(), value);
                } catch (IllegalAccessException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        result.put("refs", new LinkedHashMap<>());
        return result;
    }

    protected void applyUpdate(E entity, Map<String, Object> updates) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String fieldName = entry.getKey();
            if ("refs".equals(fieldName)) continue;
            if (metadata.readOnlyFields().contains(fieldName)) continue;
            if (metadata.hiddenFields().contains(fieldName)) continue;
            Field field = findField(entity.getClass(), fieldName);
            if (field == null) continue;
            field.setAccessible(true);
            try {
                field.set(entity, convertToFieldType(field.getType(), entry.getValue()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FIELD_VALUE",
                        "Giá trị không hợp lệ cho field '" + fieldName + "': " + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FIELD_ACCESS_ERROR",
                        "Không thể ghi vào field '" + fieldName + "'");
            }
        }
    }

    // ---- Internal helpers ----

    protected E findById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        metadata.entityClass().getSimpleName(), "id", String.valueOf(id)));
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try { return current.getDeclaredField(fieldName); }
            catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object convertToFieldType(Class<?> targetType, Object raw) {
        if (raw == null) return null;
        if (targetType.isInstance(raw)) return raw;
        String str = raw.toString();
        if (String.class.equals(targetType)) return str;
        if (Long.class.equals(targetType) || long.class.equals(targetType)) return Long.valueOf(str);
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) return Integer.valueOf(str);
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) return Boolean.valueOf(str);
        if (Instant.class.equals(targetType)) return Instant.parse(str);
        if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, str);
        return raw;
    }

    private static AdminEntityMetadata buildMetadata(Class<? extends BaseModel> entityClass, String resourceName) {
        Set<String> hiddenFields = new HashSet<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(AdminHidden.class)) {
                    hiddenFields.add(field.getName());
                }
            }
            current = current.getSuperclass();
        }

        AdminManaged managed = entityClass.getAnnotation(AdminManaged.class);
        String displayName = (managed != null && !managed.displayName().isBlank())
                ? managed.displayName()
                : entityClass.getSimpleName();

        return new AdminEntityMetadata(
                resourceName,
                displayName,
                entityClass,
                Set.copyOf(hiddenFields),
                AdminEntityMetadata.BASE_READ_ONLY);
    }
}
