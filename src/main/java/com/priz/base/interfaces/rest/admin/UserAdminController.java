package com.priz.base.interfaces.rest.admin;

import com.priz.base.application.admin.core.controller.AbstractAdminController;
import com.priz.base.application.admin.service.UserAdminService;
import com.priz.base.domain.mysql_priz_base.model.UserModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController extends AbstractAdminController<UserModel, String> {

    public UserAdminController(UserAdminService service) {
        super(service);
    }
}
