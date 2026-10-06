package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Order(2)
public class LoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingAspect.class);

    @Before("kz.iitu.springlab.aspect.Pointcuts.serviceOperation()")
    public void before(JoinPoint jp) {
        log.info("[LOG] -> {} args={}",
                 jp.getSignature().toShortString(),
                 Arrays.toString(jp.getArgs()));
    }

    @AfterReturning(
            pointcut = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()",
            returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        log.info("[LOG] <- {} returned {}",
                 jp.getSignature().toShortString(), result);
    }

    @AfterThrowing(
            pointcut = "kz.iitu.springlab.aspect.Pointcuts.serviceOperation()",
            throwing = "ex")
    public void afterThrowing(JoinPoint jp, Throwable ex) {
        log.error("[LOG] !! {} threw {}: {}",
                  jp.getSignature().toShortString(),
                  ex.getClass().getSimpleName(), ex.getMessage());
    }
}
