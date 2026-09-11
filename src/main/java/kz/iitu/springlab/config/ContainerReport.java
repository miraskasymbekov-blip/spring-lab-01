package kz.iitu.springlab.config;

import kz.iitu.springlab.notify.Notifier;
import org.slf4j.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ContainerReport implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ContainerReport.class);
    private final ApplicationContext context;

    public ContainerReport(ApplicationContext context) { this.context = context; }

    @Override
    public void run(String... args) {
        log.info("Bean definitions in total: {}", context.getBeanDefinitionCount());
        log.info("Notifier implementations: {}",
                Arrays.toString(context.getBeanNamesForType(Notifier.class)));
        log.info("Type of the notificationService bean: {}",
                context.getBean("notificationService").getClass().getName());
        Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> name.startsWith("kz.iitu") || name.contains("Notifier"))
                .forEach(name -> log.info("  bean: {}", name));
    }
}