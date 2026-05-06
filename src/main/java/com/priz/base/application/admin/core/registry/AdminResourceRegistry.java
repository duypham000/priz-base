package com.priz.base.application.admin.core.registry;

import com.priz.base.application.admin.core.service.AbstractAdminService;
import com.priz.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminResourceRegistry {

    private final Map<String, AbstractAdminService<?, ?>> services = new ConcurrentHashMap<>();

    public void register(String resource, AbstractAdminService<?, ?> service) {
        if (services.containsKey(resource)) {
            throw new IllegalStateException("Duplicate admin resource registration: " + resource);
        }
        services.put(resource, service);
    }

    public List<String> listResources() {
        return new ArrayList<>(services.keySet());
    }

    public Optional<AbstractAdminService<?, ?>> findService(String resource) {
        return Optional.ofNullable(services.get(resource));
    }

    public AbstractAdminService<?, ?> getService(String resource) {
        return findService(resource)
                .orElseThrow(() -> new ResourceNotFoundException("AdminResource", "name", resource));
    }
}
