package kz.iitu.springlab.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component("titlecase")
@Order(3)
public class TitlecaseNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TitlecaseNotifier.class);

    @PostConstruct
    public void init() {
        log.info("LIFECYCLE >> TitlecaseNotifier initialized");
    }

    @Override
    public String send(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        String result = Arrays.stream(message.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        log.info("TITLECASE >> {}", result);
        return result;
    }

    @Override
    public String channel() {
        return "titlecase";
    }
}