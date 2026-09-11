package kz.iitu.springlab.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Scope("prototype")
public class Ticket {

    private final String id = UUID.randomUUID().toString().substring(0, 8);

    public String id() { return id; }
}
