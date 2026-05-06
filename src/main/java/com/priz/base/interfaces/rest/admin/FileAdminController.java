package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.admin.core.controller.AbstractAdminController;
import com.priz.base.application.admin.service.FileAdminService;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import com.priz.common.security.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/files")
@Secured(roles = {"ADMIN"})
public class FileAdminController extends AbstractAdminController<FileModel, String> {

    public FileAdminController(FileAdminService service) {
        super(service);
    }
}
