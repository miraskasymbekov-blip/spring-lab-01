package kz.iitu.springlab.notify;

import org.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("console")
@Order(1)
public class ConsoleNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

    @Override
    public String send(String message) {
        log.info("CONSOLE >> {}", message);
        return "console: " + message;
    }

    @Override
    public String channel() { return "console"; }
}
