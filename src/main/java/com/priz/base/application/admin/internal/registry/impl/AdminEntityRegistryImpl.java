package com.priz.base.application.admin.internal.registry.impl;

import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistry;
import com.priz.common.admin.annotation.AdminHidden;
import com.priz.common.admin.annotation.AdminManaged;
import com.priz.base.common.model.BaseModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEntityRegistryImpl implements AdminEntityRegistry, SmartInitializingSingleton {

    private static final Set<String> READ_ONLY_FIELDS = Set.of(
            "id", "createdAt", "updatedAt", "createdBy", "updatedBy"
    );

    private final ApplicationContext applicationContext;
    private final EntityManager entityManager;

    private final Map<String, AdminEntityRegistration> registry = new LinkedHashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();
        Repositories repositories = new Repositories(applicationContext);

        for (EntityType<?> entityType : entityTypes) {
            Class<?> javaType = entityType.getJavaType();

            if (!BaseModel.class.isAssignableFrom(javaType)) continue;
            if (!javaType.isAnnotationPresent(AdminManaged.class)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends BaseModel> entityClass = (Class<? extends BaseModel>) javaType;

            String tableName = resolveTableName(entityClass);
            String displayName = resolveDisplayName(entityClass);
            
            Optional<Object> repoOpt = repositories.getRepositoryFor(entityClass);

            if (repoOpt.isEmpty()) {
                log.warn("[AdminRegistry] Không tìm thấy JpaRepository cho entity: {}. Bỏ qua.", entityClass.getSimpleName());
                continue;
            }
            
            Object repoObj = repoOpt.get();
            if (!(repoObj instanceof JpaRepository)) {
                log.warn("[AdminRegistry] Repository của {} không phải JpaRepository. Bỏ qua.", entityClass.getSimpleName());
                continue;
            }
            
            @SuppressWarnings("unchecked")
            JpaRepository<? extends BaseModel, String> repo = (JpaRepository<? extends BaseModel, String>) repoObj;

            if (!(repo instanceof JpaSpecificationExecutor)) {
                log.warn("[AdminRegistry] Repository của {} không implements JpaSpecificationExecutor. Bỏ qua.", entityClass.getSimpleName());
                continue;
            }

            @SuppressWarnings("unchecked")
            JpaSpecificationExecutor<? extends BaseModel> specExecutor = (JpaSpecificationExecutor<? extends BaseModel>) repo;

            Set<String> hiddenFields = collectHiddenFields(entityClass);

            AdminEntityRegistration registration = new AdminEntityRegistration(
                    tableName,
                    displayName,
                    entityClass,
                    repo,
                    specExecutor,
                    hiddenFields,
                    READ_ONLY_FIELDS
            );

            registry.put(tableName, registration);
            log.info("[AdminRegistry] Đã đăng ký entity: {} → /api/admin/tables/{}", entityClass.getSimpleName(), tableName);
        }

        log.info("[AdminRegistry] Tổng số entity đã đăng ký: {}", registry.size());
    }

    @Override
    public Optional<AdminEntityRegistration> getRegistration(String tableName) {
        return Optional.ofNullable(registry.get(tableName));
    }

    @Override
    public List<String> getRegisteredTableNames() {
        return new ArrayList<>(registry.keySet());
    }

    private String resolveDisplayName(Class<? extends BaseModel> entityClass) {
        AdminManaged adminManaged = entityClass.getAnnotation(AdminManaged.class);
        if (adminManaged != null && !adminManaged.displayName().isBlank()) {
            return adminManaged.displayName();
        }
        String name = entityClass.getSimpleName();
        if (name.endsWith("Model")) {
            name = name.substring(0, name.length() - 5);
        }
        // Split camelCase and capitalize
        return name.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private String resolveTableName(Class<? extends BaseModel> entityClass) {
        AdminManaged adminManaged = entityClass.getAnnotation(AdminManaged.class);
        if (!adminManaged.tableName().isBlank()) {
            return adminManaged.tableName();
        }
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isBlank()) {
            return tableAnnotation.name();
        }
        // Fallback: lowercase class name without "Model" suffix
        String name = entityClass.getSimpleName();
        if (name.endsWith("Model")) {
            name = name.substring(0, name.length() - 5);
        }
        return name.toLowerCase();
    }

    private Set<String> collectHiddenFields(Class<?> clazz) {
        Set<String> hidden = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(AdminHidden.class)) {
                    hidden.add(field.getName());
                }
            }
            current = current.getSuperclass();
        }
        return hidden;
    }
}
