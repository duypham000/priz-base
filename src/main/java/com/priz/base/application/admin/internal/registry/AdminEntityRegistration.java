package com.priz.base.application.admin.internal.registry;

import com.priz.base.common.model.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Set;

/**
 * Lưu toàn bộ metadata cần thiết để vận hành admin CRUD cho một entity.
 * Được tạo tự động bởi AdminEntityRegistryImpl khi startup.
 */
public record AdminEntityRegistration(

        /** Tên bảng dùng trong API path, ví dụ "users", "files" */
        String tableName,

        /** Tên hiển thị đẹp cho UI */
        String displayName,

        /** Class của JPA entity */
        Class<? extends BaseModel> entityClass,

        /** JpaRepository bean tương ứng với entity */
        JpaRepository<? extends BaseModel, String> repository,

        /** JpaSpecificationExecutor bean (thường là cùng object với repository) */
        JpaSpecificationExecutor<? extends BaseModel> specExecutor,

        /**
         * Tên các field bị ẩn bởi @AdminHidden.
         * Bị loại khỏi: response, filter, schema, update.
         */
        Set<String> hiddenFields,

        /**
         * Tên các field chỉ đọc: id, createdAt, updatedAt, createdBy, updatedBy.
         * Không cho phép cập nhật qua admin API.
         */
        Set<String> readOnlyFields
) {
}
