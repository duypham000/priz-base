package com.priz.base.application.admin.core.controller;

import com.priz.base.application.admin.core.service.AbstractAdminService;
import com.priz.base.common.model.BaseModel;
import com.priz.base.common.response.ApiResponse;
import com.priz.common.exception.BusinessException;
import com.priz.common.security.annotation.Secured;
import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

public abstract class AbstractAdminController<E extends BaseModel, ID> {

    protected final AbstractAdminService<E, ID> service;

    protected AbstractAdminController(AbstractAdminService<E, ID> service) {
        this.service = service;
    }

    @Secured(roles = {"ADMIN"})
    @GetMapping("/search-fields")
    public ResponseEntity<ApiResponse<TableSchemaResponse>> searchFields() {
        return ResponseEntity.ok(ApiResponse.success(service.getSchema()));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> filter(
            @RequestBody @Valid AdminFilterRequest request,
            @RequestParam(required = false) List<String> include) {
        return ResponseEntity.ok(ApiResponse.success(service.listRecords(request, include)));
    }

    @Secured(roles = {"ADMIN"})
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(
            @PathVariable ID id,
            @RequestParam(required = false) List<String> include) {
        return ResponseEntity.ok(ApiResponse.success(service.getRecord(id, include)));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.createRecord(data)));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        ID id = (ID) data.get("id");
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "MISSING_ID", "Field 'id' là bắt buộc trong body update");
        }
        return ResponseEntity.ok(ApiResponse.success(service.updateRecord(id, data)));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable ID id) {
        service.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/delete-batch")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@RequestBody List<ID> ids) {
        service.batchDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Secured(roles = {"ADMIN"})
    @PostMapping("/export")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"export.csv\"")
                .body(service.exportTable());
    }
}
