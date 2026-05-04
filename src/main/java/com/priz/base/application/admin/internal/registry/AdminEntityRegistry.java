package com.priz.base.application.admin.internal.registry;

import java.util.List;
import java.util.Optional;

/**
 * Registry chứa tất cả entity được đánh dấu @AdminManaged.
 * Được populate tự động khi application khởi động.
 */
public interface AdminEntityRegistry {

    /**
     * Tìm registration theo tên bảng (ví dụ "users", "files").
     * Trả về empty nếu không có entity nào được đăng ký với tên đó.
     */
    Optional<AdminEntityRegistration> getRegistration(String tableName);

    /**
     * Danh sách tất cả tên bảng đã được đăng ký vào admin system.
     */
    List<String> getRegisteredTableNames();
}
