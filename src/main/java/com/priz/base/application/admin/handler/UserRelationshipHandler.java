package com.priz.base.application.admin.handler;

import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.common.admin.handler.RelationshipHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserRelationshipHandler implements RelationshipHandler {

    private final UserRepository userRepository;

    public UserRelationshipHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<String> getNames() {
        return Set.of("user");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void populate(List<Map<String, Object>> rows, String includeName) {
        List<String> userIds = rows.stream()
                .map(row -> (String) row.get("userId"))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) return;

        List<UserModel> users = userRepository.findAllById(userIds);
        Map<String, Map<String, Object>> userById = users.stream()
                .collect(Collectors.toMap(UserModel::getId, this::toSafeMap));

        for (Map<String, Object> row : rows) {
            String userId = (String) row.get("userId");
            if (userId == null) continue;
            Map<String, Object> refs = (Map<String, Object>) row.get("refs");
            if (refs != null) {
                refs.put(includeName, userById.get(userId));
            }
        }
    }

    private Map<String, Object> toSafeMap(UserModel user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("phone", user.getPhone());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("role", user.getRole() != null ? user.getRole().name() : null);
        map.put("isActive", user.getIsActive());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
}
