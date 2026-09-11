package kz.iitu.springlab.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class TicketOffice {

    private final Ticket direct;                    // created ONCE
    private final ObjectProvider<Ticket> provider;  // source of new instances

    TicketOffice(Ticket direct, ObjectProvider<Ticket> provider) {
        this.direct = direct;
        this.provider = provider;
    }

    public Map<String, Object> demo() {
        String direct1 = direct.id();
        String direct2 = direct.id();
        String fresh1 = provider.getObject().id();
        String fresh2 = provider.getObject().id();
        return Map.of("injectedDirectly", List.of(direct1, direct2),
                "viaProvider",      List.of(fresh1, fresh2),
                "office",           System.identityHashCode(this));
    }
}
