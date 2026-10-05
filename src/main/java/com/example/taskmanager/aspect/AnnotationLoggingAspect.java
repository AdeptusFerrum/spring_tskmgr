package com.example.taskmanager.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AnnotationLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AnnotationLoggingAspect.class);

    @Pointcut("@annotation(com.example.taskmanager.annotation.Loggable)")
    public void loggableMethods() {}

    @Around("loggableMethods()")
    public Object logAnnotated(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        log.info("[LOGGABLE] → {}", method);
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[LOGGABLE] ← {} ({}ms)", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("[LOGGABLE] ✗ {} failed ({}ms): {}",
                    method, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}