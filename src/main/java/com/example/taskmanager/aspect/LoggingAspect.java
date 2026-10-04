package com.example.taskmanager.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.example.taskmanager.service.TaskService.*(..))")
    public void taskServiceMethods() {}

    @Before("taskServiceMethods()")
    public void logBefore(JoinPoint jp) {
        log.info("→ {} called with args: {}",
                jp.getSignature().getName(),
                jp.getArgs());
    }

    @After("taskServiceMethods()")
    public void logAfter(JoinPoint jp) {
        log.info("← {} finished", jp.getSignature().getName());
    }

    @Around("taskServiceMethods()")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("⏱ {} completed in {}ms", pjp.getSignature().getName(), elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("💥 {} failed after {}ms: {}",
                    pjp.getSignature().getName(), elapsed, e.getMessage());
            throw e;
        }
    }

    @AfterThrowing(pointcut = "taskServiceMethods()", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        log.error("Exception in {}.{}: {}",
                jp.getTarget().getClass().getSimpleName(),
                jp.getSignature().getName(),
                ex.getMessage());
    }
}