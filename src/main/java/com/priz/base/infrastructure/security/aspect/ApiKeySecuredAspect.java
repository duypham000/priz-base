package com.priz.base.infrastructure.security.aspect;

import com.priz.base.infrastructure.security.apikey.ApiKeyAuthenticationFilter;
import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.ApiKeyPrincipal;
import com.priz.common.security.annotation.ApiKeySecured;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Slf4j
@Aspect
@Component
@Order(2)
public class ApiKeySecuredAspect {

    @Around("@annotation(com.priz.common.security.annotation.ApiKeySecured) || "
            + "@within(com.priz.common.security.annotation.ApiKeySecured)")
    public Object validateApiKey(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        ApiKeyPrincipal principal = (ApiKeyPrincipal) request.getAttribute(
                ApiKeyAuthenticationFilter.ATTR_API_KEY_PRINCIPAL);

        if (principal == null) {
            throw new UnauthorizedException("Valid API key required");
        }

        ApiKeySecured annotation = getAnnotation(joinPoint);
        if (annotation != null && annotation.scopes().length > 0) {
            Set<String> grantedPerms = principal.getPermissions();
            String[] required = annotation.scopes();

            boolean allowed = annotation.requireAllScopes()
                    ? Arrays.stream(required).allMatch(s -> grantedPerms != null && grantedPerms.contains(s))
                    : Arrays.stream(required).anyMatch(s -> grantedPerms != null && grantedPerms.contains(s));

            if (!allowed) {
                log.warn("API key {} missing scope(s): {}", principal.getCode(),
                        Arrays.toString(required));
                throw new ForbiddenException(
                        "Insufficient scope. Required: " + Arrays.toString(required));
            }
        }

        if (annotation != null && annotation.requireSignature() && !principal.isSignatureVerified()) {
            throw new UnauthorizedException("HMAC signature required for this endpoint");
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new UnauthorizedException("No request context available");
        }
        return attrs.getRequest();
    }

    private ApiKeySecured getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        ApiKeySecured ann = method.getAnnotation(ApiKeySecured.class);
        if (ann != null) return ann;
        return joinPoint.getTarget().getClass().getAnnotation(ApiKeySecured.class);
    }
}
