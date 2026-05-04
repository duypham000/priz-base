package com.priz.base.application.admin.internal.introspector;

import com.priz.interfaces.admin.dto.FieldSchema;
import com.priz.interfaces.admin.dto.FilterOperator;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.common.admin.annotation.AdminManaged;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đọc JPA annotations và Java reflection để tạo TableSchemaResponse.
 * FE dùng kết quả này để tự sinh filter UI phù hợp với từng bảng.
 */
@Component
public class EntitySchemaIntrospector {

    public TableSchemaResponse introspect(AdminEntityRegistration registration) {
        List<FieldSchema> fields = new ArrayList<>();
        Set<String> hiddenFields = registration.hiddenFields();
        Set<String> readOnlyFields = registration.readOnlyFields();
        String primaryKey = "id"; // Default fallback

        // Walk class hierarchy: entity class → BaseModel
        List<Field> allFields = collectFields(registration.entityClass());

        for (Field field : allFields) {
            if (hiddenFields.contains(field.getName())) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;

            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = field.getName();
            }

            FieldSchema schema = buildFieldSchema(field, readOnlyFields);
            fields.add(schema);
        }

        return TableSchemaResponse.builder()
                .tableName(registration.tableName())
                .displayName(registration.displayName())
                .entityClassName(registration.entityClass().getSimpleName())
                .fields(fields)
                .primaryKey(primaryKey)
                .build();
    }

    private FieldSchema buildFieldSchema(Field field, Set<String> readOnlyFields) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        // Đọc @Column
        Column column = field.getAnnotation(Column.class);
        String columnName = (column != null && !column.name().isBlank()) ? column.name() : toSnakeCase(fieldName);
        boolean nullable = column == null || column.nullable();
        boolean columnUpdatable = column == null || column.updatable();
        int length = column != null ? column.length() : 255;

        // @Id luôn không updatable
        boolean isId = field.isAnnotationPresent(Id.class);

        // Field trong readOnlyFields không updatable
        boolean updatable = columnUpdatable && !readOnlyFields.contains(fieldName) && !isId;

        // creatable: tương tự updatable nhưng thường id (nếu auto-gen) cũng không creatable
        boolean creatable = !readOnlyFields.contains(fieldName) && !isId;

        // Detect enum
        List<String> enumValues = null;
        List<Map<String, Object>> options = null;
        boolean isEnum = fieldType.isEnum();
        if (isEnum) {
            enumValues = Arrays.stream(fieldType.getEnumConstants())
                    .map(e -> ((Enum<?>) e).name())
                    .toList();
            options = enumValues.stream()
                    .map(val -> {
                        Map<String, Object> opt = new java.util.HashMap<>();
                        opt.put("label", val);
                        opt.put("value", val);
                        return opt;
                    })
                    .toList();
        }

        // Detect type string
        String typeName = resolveTypeName(fieldType);

        // Supported operators theo type
        List<FilterOperator> operators = resolveOperators(fieldType);

        // maxLength: chỉ có ý nghĩa với String
        Integer maxLength = null;
        if (String.class.equals(fieldType) && column != null) {
            maxLength = length;
        }

        String referenceTable = null;
        if (field.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
            field.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
            Class<?> targetEntity = fieldType;
            if (targetEntity.isAnnotationPresent(AdminManaged.class)) {
                AdminManaged managed = targetEntity.getAnnotation(AdminManaged.class);
                if (!managed.tableName().isBlank()) {
                    referenceTable = managed.tableName();
                } else if (targetEntity.isAnnotationPresent(jakarta.persistence.Table.class)) {
                    referenceTable = targetEntity.getAnnotation(jakarta.persistence.Table.class).name();
                }
            } else if (targetEntity.isAnnotationPresent(jakarta.persistence.Table.class)) {
                referenceTable = targetEntity.getAnnotation(jakarta.persistence.Table.class).name();
            } else {
                referenceTable = toSnakeCase(targetEntity.getSimpleName());
            }
        }

        String displayType = isEnum ? "ENUM" : (typeName.equals("Boolean") ? "BADGE" : "TEXT");

        // Generate a friendly label by splitting camelCase and capitalizing
        String label = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2");
        label = label.substring(0, 1).toUpperCase() + label.substring(1);

        return FieldSchema.builder()
                .name(fieldName)
                .label(label)
                .columnName(columnName)
                .type(typeName)
                .nullable(nullable)
                .required(!nullable)
                .updatable(updatable)
                .creatable(creatable)
                .maxLength(maxLength)
                .enumValues(enumValues)
                .options(options)
                .supportedOperators(operators)
                .referenceTable(referenceTable)
                .displayType(displayType)
                .build();
    }

    private String resolveTypeName(Class<?> type) {
        if (String.class.equals(type)) return "String";
        if (Long.class.equals(type) || long.class.equals(type)) return "Long";
        if (Integer.class.equals(type) || int.class.equals(type)) return "Integer";
        if (Boolean.class.equals(type) || boolean.class.equals(type)) return "Boolean";
        if (Instant.class.equals(type)) return "Instant";
        if (type.isEnum()) return "Enum";
        return type.getSimpleName();
    }

    private List<FilterOperator> resolveOperators(Class<?> type) {
        if (String.class.equals(type)) {
            return List.of(
                    FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.CONTAINS, FilterOperator.NOT_CONTAINS,
                    FilterOperator.STARTS_WITH, FilterOperator.ENDS_WITH,
                    FilterOperator.LIKE, FilterOperator.EQUALS_IGNORE_CASE,
                    FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL,
                    FilterOperator.IS_EMPTY, FilterOperator.IS_NOT_EMPTY
            );
        }
        if (Long.class.equals(type) || long.class.equals(type)
                || Integer.class.equals(type) || int.class.equals(type)) {
            return List.of(
                    FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
                    FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.LESS_THAN_OR_EQUAL,
                    FilterOperator.BETWEEN,
                    FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL
            );
        }
        if (Instant.class.equals(type)) {
            return List.of(
                    FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
                    FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.LESS_THAN_OR_EQUAL,
                    FilterOperator.BETWEEN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL
            );
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return List.of(
                    FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.IS_TRUE, FilterOperator.IS_FALSE,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL
            );
        }
        if (type.isEnum()) {
            return List.of(
                    FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL
            );
        }
        // Fallback
        return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL, FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
    }

    private List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        // Thêm fields của entity class trước, rồi mới BaseModel
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current); // insert at front → BaseModel fields cuối
            current = current.getSuperclass();
        }
        // Đảo lại để BaseModel fields đứng đầu (id, createdAt...)
        for (Class<?> c : hierarchy) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
