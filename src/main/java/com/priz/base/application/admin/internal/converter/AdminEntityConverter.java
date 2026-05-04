package com.priz.base.application.admin.internal.converter;

import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.common.exception.BusinessException;
import com.priz.base.common.model.BaseModel;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class AdminEntityConverter {

    private AdminEntityConverter() {}

    /**
     * Chuyển entity thành Map<String, Object>, bỏ qua hidden fields.
     * Enum values được chuyển thành tên string.
     */
    public static Map<String, Object> toMap(BaseModel entity, Set<String> hiddenFields) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> clazz = entity.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (hiddenFields.contains(field.getName())) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.isSynthetic()) continue;

                field.setAccessible(true);
                try {
                    Object value = field.get(entity);
                    if (value instanceof Enum<?> e) {
                        value = e.name();
                    }
                    result.put(field.getName(), value);
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }

        return result;
    }

    /**
     * Áp dụng partial update lên entity, bỏ qua readOnly và hidden fields.
     * Type conversion tự động từ JSON value sang đúng Java type.
     */
    public static void applyUpdate(BaseModel entity, Map<String, Object> updates,
                                   AdminEntityRegistration registration) {
        Set<String> readOnly = registration.readOnlyFields();
        Set<String> hidden = registration.hiddenFields();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String fieldName = entry.getKey();
            if (readOnly.contains(fieldName) || hidden.contains(fieldName)) continue;

            Field field = findField(entity.getClass(), fieldName);
            if (field == null) continue;

            field.setAccessible(true);
            try {
                Object converted = convertToFieldType(field.getType(), entry.getValue());
                field.set(entity, converted);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "INVALID_FIELD_VALUE",
                        "Giá trị không hợp lệ cho field '" + fieldName + "': " + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "FIELD_ACCESS_ERROR",
                        "Không thể ghi vào field '" + fieldName + "'");
            }
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertToFieldType(Class<?> targetType, Object raw) {
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
}
