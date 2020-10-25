package org.acme.jms;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.*;
import javax.naming.Context;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A bean producing random message every 5 seconds and sending them to the Weblogic JMS queue.
 */
@ApplicationScoped
public class MessageProducer implements Runnable {
    private static final Logger LOG = Logger.getLogger(MessageProducer.class);

    @ConfigProperty(name = "queueNameForOutgoingMessages")
    String queueNameForOutgoingMessages;

    @Inject
    Context context;
    @Inject
    QueueConnectionFactory queueConnectionFactory;

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try {
            try (QueueConnection connection = queueConnectionFactory.createQueueConnection()) {
                final Queue queue = (Queue) context.lookup(queueNameForOutgoingMessages);
                final boolean transacted = true;

                try (QueueSession queueSession = connection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE)) {

                    try (QueueSender queueSender = queueSession.createSender(queue)) {
                        final String message = Integer.toString(random.nextInt(100));
                        final TextMessage textMessage = queueSession.createTextMessage(message);
                        LOG.infov("Sending message to queue [{0}]: [{1}]", queueNameForOutgoingMessages, textMessage.getText());
                        queueSender.send(textMessage);
                        queueSession.commit();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to send message to queue", e);
        }
    }
}
