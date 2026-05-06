package com.priz.base.application.admin.service;

import com.priz.base.application.admin.core.introspector.EntitySchemaIntrospector;
import com.priz.base.application.admin.core.registry.AdminResourceRegistry;
import com.priz.base.application.admin.core.service.AbstractAdminService;
import com.priz.base.application.admin.core.specification.GenericSpecificationBuilder;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import com.priz.base.domain.mysql_priz_base.repository.FileRepository;
import com.priz.common.admin.handler.RelationshipHandlerService;
import org.springframework.stereotype.Service;

@Service
public class FileAdminService extends AbstractAdminService<FileModel, String> {

    public FileAdminService(FileRepository repository,
                            EntitySchemaIntrospector introspector,
                            GenericSpecificationBuilder specBuilder,
                            RelationshipHandlerService handlerService,
                            AdminResourceRegistry registry) {
        super(repository, FileModel.class, "files", introspector, specBuilder, handlerService, registry);
    }
}
