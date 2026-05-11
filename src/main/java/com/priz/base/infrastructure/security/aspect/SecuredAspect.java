package com.priz.base.infrastructure.security.aspect;

import com.priz.base.config.security.JwtAuthenticationFilter;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.permission.PermissionCodec;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
@Order(1)
public class SecuredAspect {

    @Value("${spring.application.name:base}")
    private String applicationName;

    private static final List<String> CLASS_SUFFIXES =
            List.of("Controller", "GrpcService", "Service");

    @Around("@annotation(com.priz.common.security.annotation.Secured)")
    public Object validateSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute(JwtAuthenticationFilter.ATTR_ROLES);
        String userId = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID);

        if (userId == null) {
            log.warn("Unauthorized access attempt to: {}", joinPoint.getSignature().toShortString());
            throw new UnauthorizedException("Missing authentication for protected resource");
        }

        Secured secured = getSecuredAnnotation(joinPoint);

        if (secured != null && secured.roles().length > 0) {
            boolean hasRole = Arrays.stream(secured.roles())
                    .anyMatch(r -> roles != null && roles.contains(r));
            if (!hasRole) {
                log.warn("Access denied for user {}: required roles {}, user roles {}",
                        userId, Arrays.toString(secured.roles()), roles);
                throw new ForbiddenException(
                        "Access denied. Required roles: " + Arrays.toString(secured.roles()));
            }
        }

        if (secured != null && secured.permissions().length > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> permsMap =
                    (Map<String, Integer>) request.getAttribute(JwtAuthenticationFilter.ATTR_PERMS_MAP);

            String resourceKey = resolveResourceKey(secured, joinPoint);
            int requiredMask = PermissionCodec.combine(secured.permissions());
            int userMask = (permsMap != null) ? permsMap.getOrDefault(resourceKey, 0) : 0;

            if (!PermissionCodec.check(userMask, requiredMask, secured.requireAllPermissions())) {
                log.warn("Access denied for user {}: resource={} requiredMask={} userMask={}",
                        userId, resourceKey, requiredMask, userMask);
                throw new ForbiddenException(
                        "Access denied. Required permissions: " + Arrays.toString(secured.permissions()));
            }
        }

        SecurityContext context = SecurityContext.builder()
                .userId(userId)
                .role(roles != null && !roles.isEmpty() ? roles.get(0) : "USER")
                .build();

        try {
            SecurityContextHolder.set(context);
            MDC.put("userId", userId);
            return joinPoint.proceed();
        } finally {
            SecurityContextHolder.clear();
            MDC.remove("userId");
        }
    }

    private String resolveResourceKey(Secured secured, ProceedingJoinPoint joinPoint) {
        String key;
        if (!secured.customKey().isBlank()) {
            key = secured.customKey();
        } else {
            String simpleName = joinPoint.getTarget().getClass().getSimpleName();
            for (String suffix : CLASS_SUFFIXES) {
                if (simpleName.endsWith(suffix)) {
                    simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
                    break;
                }
            }
            key = simpleName.toLowerCase();
        }
        String prefix = secured.isGlobal() ? "global" : applicationName;
        return prefix + ":" + key;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new UnauthorizedException("No request context available");
        }
        return attrs.getRequest();
    }

    private Secured getSecuredAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Secured methodAnnotation = method.getAnnotation(Secured.class);
        if (methodAnnotation != null) return methodAnnotation;
        return joinPoint.getTarget().getClass().getAnnotation(Secured.class);
    }
}
