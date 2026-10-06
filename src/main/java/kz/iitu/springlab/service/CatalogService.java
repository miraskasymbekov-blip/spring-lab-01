package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import kz.iitu.springlab.metrics.Measured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class CatalogService {

    // self reference: the injected object is the proxy, so calls through it are intercepted
    @Autowired
    @Lazy
    private CatalogService self;

    @Measured
    public String findById(long id) {
        sleep(50);                              // simulating a database call
        return "Item no. " + id;
    }

    @Measured("catalog.findAll")
    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        sleep(300);                             // a deliberately «slow» method
        return IntStream.rangeClosed(1, limit)
                .mapToObj(i -> "Item no. " + i)
                .toList();
    }

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }

    public String removeTwice(long id) {
        String first  = self.remove(id);      // via the proxy: advice runs
        String second = self.remove(id + 1);  // via the proxy as well
        return first + "; " + second;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
