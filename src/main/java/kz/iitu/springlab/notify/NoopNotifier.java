package kz.iitu.springlab.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Fallback;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("noop")
@Fallback                                  // fallback option (Spring 6.2+)
@Order(99)
class NoopNotifier implements Notifier {

    @Override
    public String send(String message) { return "noop"; }

    @Override
    public String channel() { return "noop"; }
}
