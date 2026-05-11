package com.priz.base.application.features.permission.impl;

import com.priz.base.domain.mysql_priz_base.model.UserPermissionGroupModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupMappingRepository;
import com.priz.base.domain.mysql_priz_base.repository.PermissionGroupRepository;
import com.priz.base.domain.mysql_priz_base.repository.UserPermissionGroupRepository;
import com.priz.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceImplTest {

    @Mock
    private UserPermissionGroupRepository userGroupRepository;
    @Mock
    private PermissionGroupRepository groupRepository;
    @Mock
    private PermissionGroupMappingRepository mappingRepository;

    @InjectMocks
    private UserPermissionServiceImpl userPermissionService;

    // ---- assignGroup ----

    @Test
    void assignGroup_groupNotFound_throwsResourceNotFound() {
        when(groupRepository.existsByCode("GROUP_X")).thenReturn(false);

        assertThatThrownBy(() -> userPermissionService.assignGroup("user-1", "GROUP_X"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userGroupRepository, never()).save(any());
    }

    @Test
    void assignGroup_alreadyAssigned_throwsIllegalArgument() {
        when(groupRepository.existsByCode("BASE_ADMIN")).thenReturn(true);
        when(userGroupRepository.existsByUserIdAndPermissionGroupCode("user-1", "BASE_ADMIN"))
                .thenReturn(true);

        assertThatThrownBy(() -> userPermissionService.assignGroup("user-1", "BASE_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã có group");
        verify(userGroupRepository, never()).save(any());
    }

    @Test
    void assignGroup_validInput_savesAndReturns() {
        when(groupRepository.existsByCode("BASE_ADMIN")).thenReturn(true);
        when(userGroupRepository.existsByUserIdAndPermissionGroupCode("user-1", "BASE_ADMIN"))
                .thenReturn(false);
        UserPermissionGroupModel saved = UserPermissionGroupModel.builder()
                .userId("user-1").permissionGroupCode("BASE_ADMIN").build();
        when(userGroupRepository.save(any())).thenReturn(saved);

        UserPermissionGroupModel result = userPermissionService.assignGroup("user-1", "BASE_ADMIN");

        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getPermissionGroupCode()).isEqualTo("BASE_ADMIN");
    }

    // ---- getPermissionCodes ----

    @Test
    void getPermissionCodes_noGroups_returnsEmptySet() {
        when(userGroupRepository.findGroupCodesByUserId("user-1")).thenReturn(List.of());

        Set<String> result = userPermissionService.getPermissionCodes("user-1");

        assertThat(result).isEmpty();
        verify(mappingRepository, never()).findDistinctPermissionCodesByGroupCodes(any());
    }

    @Test
    void getPermissionCodes_withGroups_returnsPermissionsFromGroups() {
        when(userGroupRepository.findGroupCodesByUserId("user-1"))
                .thenReturn(List.of("BASE_ADMIN"));
        when(mappingRepository.findDistinctPermissionCodesByGroupCodes(List.of("BASE_ADMIN")))
                .thenReturn(List.of("manage:base:api_key", "manage:base:permission"));

        Set<String> result = userPermissionService.getPermissionCodes("user-1");

        assertThat(result).containsExactlyInAnyOrder("manage:base:api_key", "manage:base:permission");
    }

    @Test
    void getPermissionCodes_withMultipleGroups_deduplicatesPermissions() {
        when(userGroupRepository.findGroupCodesByUserId("user-1"))
                .thenReturn(List.of("GROUP_A", "GROUP_B"));
        when(mappingRepository.findDistinctPermissionCodesByGroupCodes(
                List.of("GROUP_A", "GROUP_B")))
                .thenReturn(List.of("perm:one", "perm:two", "perm:one")); // one duplicated

        Set<String> result = userPermissionService.getPermissionCodes("user-1");

        assertThat(result).hasSize(2).containsExactlyInAnyOrder("perm:one", "perm:two");
    }

    // ---- removeGroup ----

    @Test
    void removeGroup_callsRepository() {
        userPermissionService.removeGroup("user-1", "BASE_ADMIN");

        verify(userGroupRepository).deleteByUserIdAndPermissionGroupCode("user-1", "BASE_ADMIN");
    }
}
