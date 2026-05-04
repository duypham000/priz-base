package com.priz.base.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priz.base.application.admin.internal.AdminService;
import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.interfaces.admin.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AdminService adminService;

    @Test
    void listTables_should_return200_withTableNames() throws Exception {
        when(adminService.getRegisteredTables()).thenReturn(List.of("users", "files"));

        mockMvc.perform(get("/api/admin/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("users"))
                .andExpect(jsonPath("$.data[1]").value("files"));
    }

    @Test
    void getSchema_should_return200_withSchema() throws Exception {
        TableSchemaResponse schema = new TableSchemaResponse();
        when(adminService.getTableSchema("users")).thenReturn(schema);

        mockMvc.perform(get("/api/admin/tables/users/schema"))
                .andExpect(status().isOk());
    }

    @Test
    void listRecords_should_return200_withPageResponse() throws Exception {
        PageResponse<Map<String, Object>> pageResponse = PageResponse.<Map<String, Object>>builder()
                .content(List.of(Map.of("id", "user-1", "email", "test@example.com")))
                .page(1) // Changed from 0 to 1
                .pageSize(20)
                .total(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(adminService.listRecords(eq("users"), any())).thenReturn(pageResponse);

        AdminFilterRequest request = new AdminFilterRequest();

        mockMvc.perform(post("/api/admin/tables/users/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getRecord_should_return200_withRecordMap() throws Exception {
        when(adminService.getRecord("users", "user-1"))
                .thenReturn(Map.of("id", "user-1", "email", "test@example.com"));

        mockMvc.perform(get("/api/admin/tables/users/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-1"));
    }

    @Test
    void getRecord_should_return404_whenNotFound() throws Exception {
        when(adminService.getRecord("users", "nonexistent"))
                .thenThrow(new ResourceNotFoundException("UserModel", "id", "nonexistent"));

        mockMvc.perform(get("/api/admin/tables/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRecord_should_return201() throws Exception {
        Map<String, Object> data = Map.of("email", "new@example.com", "username", "newuser");
        Map<String, Object> created = Map.of("id", "new-id", "email", "new@example.com");

        when(adminService.createRecord(eq("users"), any())).thenReturn(created);

        mockMvc.perform(post("/api/admin/tables/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("new-id"));
    }

    @Test
    void updateRecord_should_return200() throws Exception {
        Map<String, Object> data = Map.of("fullName", "Updated Name");
        Map<String, Object> updated = Map.of("id", "user-1", "fullName", "Updated Name");

        when(adminService.updateRecord(eq("users"), eq("user-1"), any())).thenReturn(updated);

        mockMvc.perform(put("/api/admin/tables/users/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    void deleteRecord_should_return200() throws Exception {
        mockMvc.perform(delete("/api/admin/tables/users/user-1"))
                .andExpect(status().isOk());
    }
}
