package com.priz.base.interfaces.rest;

import com.priz.base.application.admin.internal.AdminService;
import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Generic admin CRUD — tự động cho mọi entity gắn @AdminManaged")
@Secured(roles = "ADMIN")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/tables")
    @Operation(summary = "Danh sách tên các bảng đã đăng ký vào admin system")
    public ResponseEntity<ApiResponse<List<String>>> listTables() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRegisteredTables()));
    }

    @GetMapping("/tables/{tableName}/schema")
    @Operation(summary = "Schema metadata của một bảng — FE dùng để tự sinh filter UI")
    public ResponseEntity<ApiResponse<TableSchemaResponse>> getSchema(
            @PathVariable String tableName) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTableSchema(tableName)));
    }

    @PostMapping("/tables/{tableName}/query")
    @Operation(summary = "Query records với dynamic filter và pagination")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> listRecords(
            @PathVariable String tableName,
            @Valid @RequestBody AdminFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listRecords(tableName, request)));
    }

    @GetMapping("/tables/{tableName}/{id}")
    @Operation(summary = "Lấy một record theo ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecord(
            @PathVariable String tableName,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRecord(tableName, id)));
    }

    @PostMapping("/tables/{tableName}")
    @Operation(summary = "Tạo record mới")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRecord(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.status(201).body(ApiResponse.created(adminService.createRecord(tableName, data)));
    }

    @PutMapping("/tables/{tableName}/{id}")
    @Operation(summary = "Cập nhật partial record")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRecord(
            @PathVariable String tableName,
            @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateRecord(tableName, id, data)));
    }

    @DeleteMapping("/tables/{tableName}/{id}")
    @Operation(summary = "Xóa record theo ID")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @PathVariable String tableName,
            @PathVariable String id) {
        adminService.deleteRecord(tableName, id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa record thành công", null));
    }

    @PostMapping("/tables/{tableName}/batch-delete")
    @Operation(summary = "Xóa nhiều records cùng lúc")
    public ResponseEntity<ApiResponse<Void>> batchDelete(
            @PathVariable String tableName,
            @RequestBody List<String> ids) {
        adminService.batchDelete(tableName, ids);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa " + ids.size() + " records thành công", null));
    }

    @GetMapping("/tables/{tableName}/export")
    @Operation(summary = "Export dữ liệu bảng ra file CSV")
    public ResponseEntity<String> exportTable(@PathVariable String tableName) {
        String csv = adminService.exportTable(tableName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tableName + "_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
