package com.priz.base.infrastructure.security.aspect;

import com.priz.common.exception.ForbiddenException;
import com.priz.common.exception.UnauthorizedException;
import com.priz.common.security.SecurityContext;
import com.priz.common.security.SecurityContextHolder;
import com.priz.common.security.annotation.Secured;
import com.priz.common.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class SecuredAspect {

    private final JwtService jwtService;

    @Around("@annotation(com.priz.common.security.annotation.Secured) || "
            + "@within(com.priz.common.security.annotation.Secured)")
    public Object validateSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            HttpServletRequest request = getCurrentRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            if (!jwtService.validateToken(token)) {
                throw new UnauthorizedException("Invalid or expired token");
            }

            Claims claims = jwtService.parseToken(token);
            SecurityContext context = SecurityContext.builder()
                    .userId(claims.getSubject())
                    .email(claims.get("email", String.class))
                    .username(claims.get("username", String.class))
                    .role(claims.get("role", String.class))
                    .build();

            Secured secured = getSecuredAnnotation(joinPoint);
            if (secured != null && secured.roles().length > 0) {
                boolean hasRole = Arrays.asList(secured.roles()).contains(context.getRole());
                if (!hasRole) {
                    throw new ForbiddenException(
                            "Access denied. Required roles: " + Arrays.toString(secured.roles()));
                }
            }

            SecurityContextHolder.set(context);
            MDC.put("userId", context.getUserId());

            return joinPoint.proceed();

        } finally {
            SecurityContextHolder.clear();
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
