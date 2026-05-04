package com.priz.base.application.admin.internal;

import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.interfaces.admin.dto.PageResponse;

import java.util.List;
import java.util.Map;

public interface AdminService {

    /** Danh sách tất cả tên bảng đã được đăng ký vào admin system. */
    List<String> getRegisteredTables();

    /** Schema metadata của một bảng để FE tự sinh filter UI. */
    TableSchemaResponse getTableSchema(String tableName);

    /** Danh sách records với dynamic filter và pagination. */
    PageResponse<Map<String, Object>> listRecords(String tableName, AdminFilterRequest request);

    /** Lấy một record theo ID. */
    Map<String, Object> getRecord(String tableName, String id);

    /** Tạo record mới. */
    Map<String, Object> createRecord(String tableName, Map<String, Object> data);

    /** Cập nhật partial record. */
    Map<String, Object> updateRecord(String tableName, String id, Map<String, Object> data);

    /** Xóa record theo ID. */
    void deleteRecord(String tableName, String id);

    /** Xóa nhiều records cùng lúc. */
    void batchDelete(String tableName, List<String> ids);

    /** Export toàn bộ bảng ra file CSV. */
    String exportTable(String tableName);
}
