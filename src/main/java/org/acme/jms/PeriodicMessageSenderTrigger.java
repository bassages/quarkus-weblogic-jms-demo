package org.acme.jms;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A bean producing random message every 5 seconds and sending them to the Weblogic JMS queue.
 */
@ApplicationScoped
public class PeriodicMessageSenderTrigger implements Runnable {
    @Inject
    MessageSenderService messageSenderService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Transactional(rollbackOn = RuntimeException.class)
    @Override
    public void run() {
        try {
            messageSenderService.send();
        } catch (ForceRollbackException e) {
            // ignore
        }
    }
}
