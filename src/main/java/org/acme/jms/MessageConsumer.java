package org.acme.jms;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A bean consuming messages from a Weblogic JMS queue.
 */
@ApplicationScoped
public class MessageConsumer {
    private static final Logger LOG = Logger.getLogger(MessageConsumer.class);

    @ConfigProperty(name = "queueNameForIncomingMessages")
    String queueNameForIncomingMessages;

    @ConfigProperty(name = "jmsConnectionFactoryName")
    String jmsConnectionFactoryName;

    private boolean stop = false;

    void onStart(@Observes StartupEvent ev) {
        try {
            final Context context = getContext();
            final QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) context.lookup(jmsConnectionFactoryName);

            try (QueueConnection queueConnection = queueConnectionFactory.createQueueConnection()) {
                queueConnection.start();

                final Queue queue = (Queue) context.lookup(queueNameForIncomingMessages);
                final boolean transacted = false;

                try (QueueSession queueSession = queueConnection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE)) {
                    try (QueueReceiver queueReceiver = queueSession.createReceiver(queue)) {
                        while (!stop) {
                            receiveNextMessage(queueReceiver);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Faild to receive messages from queue", e);
        }
    }

    private void receiveNextMessage(QueueReceiver queueReceiver) throws JMSException {
        final Message message = queueReceiver.receiveNoWait();
        if (message != null) {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                LOG.infov("Received message from queue: {0}", textMessage.getText());
            } else {
                LOG.infov("Received non-textmessage from queue: {0}", message);
            }
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
