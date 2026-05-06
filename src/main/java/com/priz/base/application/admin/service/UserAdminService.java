package com.priz.base.application.admin.service;

import com.priz.base.application.admin.core.introspector.EntitySchemaIntrospector;
import com.priz.base.application.admin.core.registry.AdminResourceRegistry;
import com.priz.base.application.admin.core.service.AbstractAdminService;
import com.priz.base.application.admin.core.specification.GenericSpecificationBuilder;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import com.priz.base.domain.mysql_priz_base.repository.UserRepository;
import com.priz.common.admin.handler.RelationshipHandlerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserAdminService extends AbstractAdminService<UserModel, String> {

    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository repository,
                            EntitySchemaIntrospector introspector,
                            GenericSpecificationBuilder specBuilder,
                            RelationshipHandlerService handlerService,
                            AdminResourceRegistry registry,
                            PasswordEncoder passwordEncoder) {
        super(repository, UserModel.class, "users", introspector, specBuilder, handlerService, registry);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void applyUpdate(UserModel entity, Map<String, Object> updates) {
        String rawPassword = (String) updates.get("password");
        if (rawPassword != null && !rawPassword.isBlank()) {
            entity.setPassword(passwordEncoder.encode(rawPassword));
        }
        super.applyUpdate(entity, updates);
    }
}
