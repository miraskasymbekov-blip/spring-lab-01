package kz.iitu.springlab.lifecycle;

import jakarta.annotation.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class LifecycleDemo {

    private static final Logger log = LoggerFactory.getLogger(LifecycleDemo.class);

    private final DateTimeFormatter formatter;
    private final List<String> events = new ArrayList<>();

    public LifecycleDemo(DateTimeFormatter formatter) {   // the bean from task 4
        this.formatter = formatter;
        record("1. Constructor called");
    }

    @PostConstruct
    void init() { record("2. @PostConstruct executed"); }

    @PreDestroy
    void shutdown() { record("3. @PreDestroy executed"); }

    public List<String> events() { return List.copyOf(events); }

    private void record(String stage) {
        String line = LocalDateTime.now().format(formatter) + "  " + stage;
        events.add(line);
        log.info("LIFECYCLE >> {}", line);
    }
}
