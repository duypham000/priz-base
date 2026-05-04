package com.priz.base.application.admin.internal.specification;

import com.priz.interfaces.admin.dto.FilterCondition;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.common.exception.BusinessException;
import com.priz.base.common.model.BaseModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chuyển đổi danh sách FilterCondition thành JPA Specification động.
 * Tất cả conditions được kết hợp với AND.
 */
@Component
public class GenericSpecificationBuilder {

    public Specification<BaseModel> build(List<FilterCondition> conditions, AdminEntityRegistration registration) {
        if (conditions == null || conditions.isEmpty()) {
            return (root, query, cb) -> null;
        }

        // Build field type map để convert values
        Map<String, Class<?>> fieldTypes = buildFieldTypeMap(registration.entityClass());

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (FilterCondition condition : conditions) {
                String field = condition.getField();

                // Validate field tồn tại
                if (!fieldTypes.containsKey(field)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "INVALID_FILTER_FIELD",
                            "Field không tồn tại: '" + field + "' trên bảng '" + registration.tableName() + "'");
                }

                // Validate field không bị ẩn
                if (registration.hiddenFields().contains(field)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "HIDDEN_FILTER_FIELD",
                            "Không được filter theo field ẩn: '" + field + "'");
                }

                Class<?> fieldType = fieldTypes.get(field);
                Path<Object> path = root.get(field);

                Predicate predicate = buildPredicate(condition, path, fieldType, cb, registration);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(FilterCondition condition, Path<Object> path,
                                      Class<?> fieldType, CriteriaBuilder cb,
                                      AdminEntityRegistration registration) {
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

            case IN -> {
                List<Object> convertedValues = convertValues(rawValues, fieldType);
                yield path.in(convertedValues);
            }

            case NOT_IN -> {
                List<Object> convertedValues = convertValues(rawValues, fieldType);
                yield cb.not(path.in(convertedValues));
            }

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
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "INVALID_FILTER_VALUES",
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
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    map.put(field.getName(), field.getType());
                }
            }
            current = current.getSuperclass();
        }
        return map;
    }
}
