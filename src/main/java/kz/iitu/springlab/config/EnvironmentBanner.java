package kz.iitu.springlab.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

public interface EnvironmentBanner {
    String describe();
}

@Component
@Profile("dev")
class DevBanner implements EnvironmentBanner {
    public String describe() { return "DEVELOPMENT: the data is test data"; }
}

@Component
@Profile("prod")
class ProdBanner implements EnvironmentBanner {
    public String describe() { return "PRODUCTION: handle with care"; }
}

@Component
@Profile("!dev & !prod")                 // when no profile is set
class DefaultBanner implements EnvironmentBanner {
    public String describe() { return "no profile is active"; }
}
