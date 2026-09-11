package kz.iitu.springlab.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificationService {

    private final Notifier primary;              // @Primary -> EmailNotifier
    private final Notifier console;              // stated explicitly
    private final List<Notifier> all;            // all of them, ordered by @Order
    private final Map<String, Notifier> byName;  // key is the bean name

    public NotificationService(Notifier primary,
                               @Qualifier("console") Notifier console,
                               List<Notifier> all,
                               Map<String, Notifier> byName) {
        this.primary = primary;
        this.console = console;
        this.all = all;
        this.byName = byName;
    }

    public String viaPrimary(String message) { return primary.send(message); }

    public String viaConsole(String message) { return console.send(message); }

    public List<String> viaAll(String message) {
        return all.stream().map(n -> n.send(message)).toList();
    }

    public Set<String> names() { return byName.keySet(); }
}
