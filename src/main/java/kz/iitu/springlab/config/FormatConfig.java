package kz.iitu.springlab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Configuration
public class FormatConfig {

    @Bean
    public DateTimeFormatter reportDateFormatter(
            @Value("${app.date-pattern:dd.MM.yyyy HH:mm:ss}") String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
    }
}
