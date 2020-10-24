package org.acme.jms;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
            Context context = getContext();
            final QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) context.lookup("jms/connectionfactory");
            try (QueueConnection connection = queueConnectionFactory.createQueueConnection()) {
                final Queue queue = (Queue) context.lookup("jms/queue0");
                try (QueueSession queueSession = connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE)) {
                    try (QueueSender queueSender = queueSession.createSender(queue)) {
                        final String message = Integer.toString(random.nextInt(100));
                        final TextMessage textMessage = queueSession.createTextMessage(message);
                        LOG.infov("Sending message to queue: {0}", textMessage.getText());
                        queueSender.send(textMessage);
                        queueSession.commit();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Faild to send message to queue", e);
        }
    }

    private Context getContext() throws NamingException {
        Context context = new InitialContext();
        context.addToEnvironment(Context.INITIAL_CONTEXT_FACTORY,"weblogic.jndi.WLInitialContextFactory");
        context.addToEnvironment(Context.PROVIDER_URL,"t3://localhost:7001");
        context.addToEnvironment(Context.SECURITY_CREDENTIALS,"weblogic1");
        context.addToEnvironment(Context.SECURITY_PRINCIPAL,"weblogic1");
        return context;
    }
}
