package com.priz.base.application.admin.core.specification;

import com.priz.base.application.admin.core.AdminEntityMetadata;
import com.priz.base.common.model.BaseModel;
import com.priz.common.exception.BusinessException;
import com.priz.interfaces.admin.dto.FilterCondition;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GenericSpecificationBuilder {

    public Specification<BaseModel> build(List<FilterCondition> conditions, AdminEntityMetadata metadata) {
        if (conditions == null || conditions.isEmpty()) {
            return (root, query, cb) -> null;
        }

        Map<String, Class<?>> fieldTypes = buildFieldTypeMap(metadata.entityClass());

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (FilterCondition condition : conditions) {
                String field = condition.getField();

                if (!fieldTypes.containsKey(field)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FILTER_FIELD",
                            "Field không tồn tại: '" + field + "' trên resource '" + metadata.resourceName() + "'");
                }
                if (metadata.hiddenFields().contains(field)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "HIDDEN_FILTER_FIELD",
                            "Không được filter theo field ẩn: '" + field + "'");
                }

                Class<?> fieldType = fieldTypes.get(field);
                Path<Object> path = root.get(field);
                Predicate predicate = buildPredicate(condition, path, fieldType, cb);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(FilterCondition condition, Path<Object> path,
                                      Class<?> fieldType, CriteriaBuilder cb) {
        Object rawValue = condition.getValue();
        Object rawValueTo = condition.getValueTo();
        List<Object> rawValues = condition.getValues();

        return switch (condition.getOperator()) {
            case EQUAL -> cb.equal(path, convertValue(rawValue, fieldType));
            case NOT_EQUAL -> cb.notEqual(path, convertValue(rawValue, fieldType));
            case CONTAINS -> cb.like(cb.lower(path.as(String.class)),
                    "%" + rawValue.toString().toLowerCase() + "%");
            case NOT_CONTAINS -> cb.notLike(cb.lower(path.as(String.class)),
                    "%" + rawValue.toString().toLowerCase() + "%");
            case STARTS_WITH -> cb.like(cb.lower(path.as(String.class)),
                    rawValue.toString().toLowerCase() + "%");
            case ENDS_WITH -> cb.like(cb.lower(path.as(String.class)),
                    "%" + rawValue.toString().toLowerCase());
            case LIKE -> cb.like(path.as(String.class), rawValue.toString());
            case EQUALS_IGNORE_CASE -> cb.equal(cb.lower(path.as(String.class)),
                    rawValue.toString().toLowerCase());
            case GREATER_THAN -> cb.greaterThan(path.as(Comparable.class),
                    (Comparable) convertValue(rawValue, fieldType));
            case LESS_THAN -> cb.lessThan(path.as(Comparable.class),
                    (Comparable) convertValue(rawValue, fieldType));
            case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(path.as(Comparable.class),
                    (Comparable) convertValue(rawValue, fieldType));
            case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo(path.as(Comparable.class),
                    (Comparable) convertValue(rawValue, fieldType));
            case BETWEEN -> cb.between(path.as(Comparable.class),
                    (Comparable) convertValue(rawValue, fieldType),
                    (Comparable) convertValue(rawValueTo, fieldType));
            case IN -> path.in(convertValues(rawValues, fieldType));
            case NOT_IN -> cb.not(path.in(convertValues(rawValues, fieldType)));
            case IS_NULL -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
            case IS_TRUE -> cb.isTrue(path.as(Boolean.class));
            case IS_FALSE -> cb.isFalse(path.as(Boolean.class));
            case IS_EMPTY -> cb.or(cb.isNull(path), cb.equal(path.as(String.class), ""));
            case IS_NOT_EMPTY -> cb.and(cb.isNotNull(path), cb.notEqual(path.as(String.class), ""));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(Object raw, Class<?> targetType) {
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

    private List<Object> convertValues(List<Object> rawValues, Class<?> targetType) {
        if (rawValues == null || rawValues.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FILTER_VALUES",
                    "Operator IN/NOT_IN yêu cầu danh sách values không rỗng");
        }
        List<Object> result = new ArrayList<>();
        for (Object raw : rawValues) {
            result.add(convertValue(raw, targetType));
        }
        return result;
    }

    private Map<String, Class<?>> buildFieldTypeMap(Class<?> entityClass) {
        Map<String, Class<?>> map = new HashMap<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    map.put(field.getName(), field.getType());
                }
            }
            current = current.getSuperclass();
        }
        return map;
    }
}
