package kz.iitu.springlab.aspect;

import kz.iitu.springlab.metrics.Measured;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Individual variant 11: metrics collected for methods annotated with @Measured. */
@Aspect
@Component
@Order(4)
public class MeasuredAspect {

    private static final Logger log = LoggerFactory.getLogger(MeasuredAspect.class);

    private static final class Stats {
        final LongAdder count = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
    }

    private final Map<String, Stats> metrics = new ConcurrentHashMap<>();

    @Around("@annotation(measured)")
    public Object measure(ProceedingJoinPoint pjp, Measured measured) throws Throwable {
        String name = measured.value().isEmpty()
                ? pjp.getSignature().toShortString() : measured.value();
        long started = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            Stats s = metrics.computeIfAbsent(name, k -> new Stats());
            s.count.increment();
            s.totalNanos.add(System.nanoTime() - started);
            log.info("[METRIC] {} recorded", name);
        }
    }

    public Map<String, Map<String, Object>> summary() {
        Map<String, Map<String, Object>> out = new TreeMap<>();
        metrics.forEach((name, s) -> {
            long count = s.count.sum();
            double totalMs = s.totalNanos.sum() / 1_000_000.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("count", count);
            row.put("totalMs", Math.round(totalMs * 100) / 100.0);
            row.put("avgMs", count == 0 ? 0 : Math.round(totalMs / count * 100) / 100.0);
            out.put(name, row);
        });
        return out;
    }
}
