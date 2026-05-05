package com.priz.base.application.admin.internal.impl;

import com.priz.interfaces.admin.dto.AdminFilterRequest;
import com.priz.interfaces.admin.dto.TableSchemaResponse;
import com.priz.base.application.admin.internal.introspector.EntitySchemaIntrospector;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistration;
import com.priz.base.application.admin.internal.registry.AdminEntityRegistry;
import com.priz.base.application.admin.internal.specification.GenericSpecificationBuilder;
import com.priz.common.exception.ResourceNotFoundException;
import com.priz.base.common.model.BaseModel;
import com.priz.interfaces.admin.dto.PageRequestDto;
import com.priz.interfaces.admin.dto.PageResponse;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminEntityRegistry registry;

    @Mock
    private EntitySchemaIntrospector introspector;

    @Mock
    private GenericSpecificationBuilder specBuilder;

    @InjectMocks
    private AdminServiceImpl adminService;

    // =============================================
    // GET REGISTERED TABLES
    // =============================================

    @Test
    void getRegisteredTables_should_delegateToRegistry() {
        List<String> tables = List.of("users", "files", "refresh_tokens");
        when(registry.getRegisteredTableNames()).thenReturn(tables);

        List<String> result = adminService.getRegisteredTables();

        assertEquals(tables, result);
    }

    // =============================================
    // GET TABLE SCHEMA
    // =============================================

    @Test
    void getTableSchema_should_returnIntrospectedSchema() {
        AdminEntityRegistration reg = createMockRegistration();
        TableSchemaResponse schema = new TableSchemaResponse();

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        when(introspector.introspect(reg)).thenReturn(schema);

        TableSchemaResponse result = adminService.getTableSchema("users");

        assertSame(schema, result);
    }

    @Test
    void getTableSchema_should_throwNotFound_whenTableNotRegistered() {
        when(registry.getRegistration("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.getTableSchema("nonexistent"));
    }

    // =============================================
    // LIST RECORDS
    // =============================================

    @Test
    @SuppressWarnings("unchecked")
    void listRecords_should_returnPageResponse() {
        AdminEntityRegistration reg = createMockRegistration();
        UserModel user = new UserModel();
        user.setId("user-1");
        Page<BaseModel> page = new PageImpl<>(List.of(user));

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        when(specBuilder.build(anyList(), eq(reg))).thenReturn((Specification<BaseModel>) (root, query, cb) -> null);

        JpaSpecificationExecutor<BaseModel> specExec = (JpaSpecificationExecutor<BaseModel>) reg.specExecutor();
        when(specExec.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AdminFilterRequest request = AdminFilterRequest.builder()
                .pagination(new PageRequestDto())
                .build();

        PageResponse<Map<String, Object>> result = adminService.listRecords("users", request);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // =============================================
    // GET RECORD
    // =============================================

    @Test
    @SuppressWarnings("unchecked")
    void getRecord_should_returnMap_whenFound() {
        AdminEntityRegistration reg = createMockRegistration();
        UserModel user = new UserModel();
        user.setId("user-1");

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        when(repo.findById("user-1")).thenReturn(Optional.of(user));

        Map<String, Object> result = adminService.getRecord("users", "user-1");

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRecord_should_throwNotFound_whenMissing() {
        AdminEntityRegistration reg = createMockRegistration();

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.getRecord("users", "nonexistent"));
    }

    // =============================================
    // DELETE RECORD
    // =============================================

    @Test
    @SuppressWarnings("unchecked")
    void deleteRecord_should_deleteById() {
        AdminEntityRegistration reg = createMockRegistration();
        UserModel user = new UserModel();
        user.setId("user-1");

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        when(repo.findById("user-1")).thenReturn(Optional.of(user));

        adminService.deleteRecord("users", "user-1");

        verify(repo).deleteById("user-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteRecord_should_throwNotFound_whenMissing() {
        AdminEntityRegistration reg = createMockRegistration();

        when(registry.getRegistration("users")).thenReturn(Optional.of(reg));
        JpaRepository<BaseModel, String> repo = (JpaRepository<BaseModel, String>) reg.repository();
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.deleteRecord("users", "nonexistent"));
    }

    // =============================================
    // HELPER
    // =============================================

    @SuppressWarnings("unchecked")
    private AdminEntityRegistration createMockRegistration() {
        JpaRepository<? extends BaseModel, String> repo = mock(JpaRepository.class);
        JpaSpecificationExecutor<? extends BaseModel> specExec = mock(JpaSpecificationExecutor.class);

        return new AdminEntityRegistration(
                "users",
                "Users",
                UserModel.class,
                repo,
                specExec,
                Set.of("password"),
                Set.of("id", "createdAt", "updatedAt", "createdBy", "updatedBy")
        );
    }
}
