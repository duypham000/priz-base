package com.priz.base.infrastructure.security.aspect;

import com.priz.base.config.security.JwtAuthenticationFilter;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import com.priz.common.security.annotation.Secured;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@Order(1)
public class SecuredAspect {

    @Around("@annotation(com.priz.common.security.annotation.Secured) || "
            + "@within(com.priz.common.security.annotation.Secured)")
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
            boolean hasRequiredRole = Arrays.stream(secured.roles())
                    .anyMatch(reqRole -> roles != null && roles.contains(reqRole));
            if (!hasRequiredRole) {
                log.warn("Access denied for user {}: required roles {}, user roles {}",
                        userId, Arrays.toString(secured.roles()), roles);
                throw new ForbiddenException(
                        "Access denied. Required roles: " + Arrays.toString(secured.roles()));
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
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return joinPoint.getTarget().getClass().getAnnotation(Secured.class);
    }
}
