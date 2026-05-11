package com.priz.base.application.features.permission;

import com.priz.base.domain.mysql_priz_base.model.PermissionModel;
import com.priz.base.domain.mysql_priz_base.repository.PermissionRepository;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PermissionScannerRunner implements ApplicationRunner {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PermissionRepository permissionRepository;

    @Value("${spring.application.name:base}")
    private String applicationName;

    @Value("${app.permission.auto-discovery:true}")
    private boolean autoDiscoveryEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoDiscoveryEnabled) {
            log.info("Permission auto-discovery disabled");
            return;
        }

        // Map of (customKey -> OR-combined actionMask) discovered from @Secured annotations
        Map<String, Integer> discovered = new HashMap<>();

        requestMappingHandlerMapping.getHandlerMethods().forEach((info, handlerMethod) -> {
            Method method = handlerMethod.getMethod();
            Class<?> controllerClass = handlerMethod.getBeanType();

            Secured secured = method.getAnnotation(Secured.class);
            if (secured == null) {
                secured = controllerClass.getAnnotation(Secured.class);
            }
            if (secured == null || secured.permissions().length == 0) return;

            String key = secured.customKey().isBlank()
                    ? controllerClass.getSimpleName().toLowerCase()
                    : secured.customKey();
            String prefix = secured.isGlobal() ? "global" : applicationName;
            String fullKey = prefix + ":" + key;

            int mask = PermissionCodec.combine(secured.permissions());
            discovered.merge(fullKey, mask, (a, b) -> a | b);
        });

        int upserted = 0;
        for (Map.Entry<String, Integer> entry : discovered.entrySet()) {
            String customKey = entry.getKey();
            int actionMask = entry.getValue();
            String code = "auto:" + customKey + ":" + actionMask;

            if (!permissionRepository.existsByCode(code)) {
                permissionRepository.save(PermissionModel.builder()
                        .code(code)
                        .name("Auto: " + customKey)
                        .groupCode("auto")
                        .customKey(customKey)
                        .actionMask(actionMask)
                        .build());
                upserted++;
            }
        }

        log.info("Permission auto-discovery: scanned {} resources, upserted {} new entries",
                discovered.size(), upserted);
    }
}
