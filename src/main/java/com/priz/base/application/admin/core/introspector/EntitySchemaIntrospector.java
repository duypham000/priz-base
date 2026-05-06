package com.priz.base.application.admin.core.introspector;

import com.priz.base.application.admin.core.AdminEntityMetadata;
import com.priz.common.admin.annotation.AdminManaged;
import com.priz.common.admin.annotation.AdminRelation;
import com.priz.interfaces.admin.dto.FieldSchema;
import com.priz.interfaces.admin.dto.FilterOperator;
import com.priz.interfaces.admin.dto.RelationshipSchema;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EntitySchemaIntrospector {

    public TableSchemaResponse introspect(AdminEntityMetadata metadata) {
        List<FieldSchema> fields = new ArrayList<>();
        List<RelationshipSchema> availableIncludes = new ArrayList<>();
        Set<String> hiddenFields = metadata.hiddenFields();
        Set<String> readOnlyFields = metadata.readOnlyFields();
        String primaryKey = "id";

        List<Field> allFields = collectFields(metadata.entityClass());

        for (Field field : allFields) {
            if (hiddenFields.contains(field.getName())) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;

            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = field.getName();
            }

            if (field.isAnnotationPresent(AdminRelation.class)) {
                AdminRelation rel = field.getAnnotation(AdminRelation.class);
                availableIncludes.add(new RelationshipSchema(
                        rel.name(), field.getName(), rel.targetResource(), rel.displayLabel()));
            }

            fields.add(buildFieldSchema(field, readOnlyFields));
        }

        return TableSchemaResponse.builder()
                .tableName(metadata.resourceName())
                .displayName(metadata.displayName())
                .entityClassName(metadata.entityClass().getSimpleName())
                .fields(fields)
                .primaryKey(primaryKey)
                .availableIncludes(availableIncludes)
                .build();
    }

    private FieldSchema buildFieldSchema(Field field, Set<String> readOnlyFields) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        Column column = field.getAnnotation(Column.class);
        String columnName = (column != null && !column.name().isBlank()) ? column.name() : toSnakeCase(fieldName);
        boolean nullable = column == null || column.nullable();
        boolean columnUpdatable = column == null || column.updatable();
        int length = column != null ? column.length() : 255;

        boolean isId = field.isAnnotationPresent(Id.class);
        boolean updatable = columnUpdatable && !readOnlyFields.contains(fieldName) && !isId;
        boolean creatable = !readOnlyFields.contains(fieldName) && !isId;

        List<String> enumValues = null;
        List<Map<String, Object>> options = null;
        boolean isEnum = fieldType.isEnum();
        if (isEnum) {
            enumValues = Arrays.stream(fieldType.getEnumConstants())
                    .map(e -> ((Enum<?>) e).name())
                    .toList();
            options = enumValues.stream()
                    .map(val -> {
                        Map<String, Object> opt = new HashMap<>();
                        opt.put("label", val);
                        opt.put("value", val);
                        return opt;
                    })
                    .toList();
        }

        String typeName = resolveTypeName(fieldType);
        List<FilterOperator> operators = resolveOperators(fieldType);

        Integer maxLength = null;
        if (String.class.equals(fieldType) && column != null) {
            maxLength = length;
        }

        String referenceTable = resolveReferenceTable(field, fieldType);
        String displayType = isEnum ? "ENUM" : (typeName.equals("Boolean") ? "BADGE" : "TEXT");

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

    private String resolveReferenceTable(Field field, Class<?> fieldType) {
        if (!field.isAnnotationPresent(jakarta.persistence.ManyToOne.class) &&
            !field.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
            return null;
        }
        if (fieldType.isAnnotationPresent(AdminManaged.class)) {
            AdminManaged managed = fieldType.getAnnotation(AdminManaged.class);
            if (!managed.tableName().isBlank()) return managed.tableName();
        }
        if (fieldType.isAnnotationPresent(jakarta.persistence.Table.class)) {
            return fieldType.getAnnotation(jakarta.persistence.Table.class).name();
        }
        return toSnakeCase(fieldType.getSimpleName());
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
            return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.CONTAINS, FilterOperator.NOT_CONTAINS,
                    FilterOperator.STARTS_WITH, FilterOperator.ENDS_WITH,
                    FilterOperator.LIKE, FilterOperator.EQUALS_IGNORE_CASE,
                    FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL,
                    FilterOperator.IS_EMPTY, FilterOperator.IS_NOT_EMPTY);
        }
        if (Long.class.equals(type) || long.class.equals(type)
                || Integer.class.equals(type) || int.class.equals(type)) {
            return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
                    FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.LESS_THAN_OR_EQUAL,
                    FilterOperator.BETWEEN, FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
        }
        if (Instant.class.equals(type)) {
            return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
                    FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.LESS_THAN_OR_EQUAL,
                    FilterOperator.BETWEEN, FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.IS_TRUE, FilterOperator.IS_FALSE,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
        }
        if (type.isEnum()) {
            return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                    FilterOperator.IN, FilterOperator.NOT_IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
        }
        return List.of(FilterOperator.EQUAL, FilterOperator.NOT_EQUAL,
                FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
    }

    private List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        for (Class<?> c : hierarchy) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
