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

/**
 * A bean consuming messages from a Weblogic JMS queue.
 */
@ApplicationScoped
public class MessageConsumer implements MessageListener, ExceptionListener {
    private static final Logger LOG = Logger.getLogger(MessageConsumer.class);

    @ConfigProperty(name = "queueNameForIncomingMessages")
    String queueNameForIncomingMessages;

    @Inject
    Context context;
    @Inject
    QueueConnectionFactory queueConnectionFactory;

    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueReceiver queueReceiver;

    void onStart(@Observes StartupEvent ev) {
        LOG.infov("Start listenening for messages on queue [{0}]", queueNameForIncomingMessages);
        try {
            queueConnection = queueConnectionFactory.createQueueConnection();
            queueConnection.setExceptionListener(this);
            queueConnection.start();

            final Queue queue = (Queue) context.lookup(queueNameForIncomingMessages);
            final boolean transacted = false;

            queueSession = queueConnection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
            queueReceiver = queueSession.createReceiver(queue);
            queueReceiver.setMessageListener(this);

        } catch (Exception e) {
            LOG.error("Faild to start receiving messages from queue", e);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        try {
            LOG.infov("Closing connection to queue [{0}]", queueNameForIncomingMessages);
            // Close the queue connection. This will in turn close both the session and the QueueSender.
            queueConnection.close();
        } catch (JMSException e) {
            LOG.error("Error while closing the connection", e);
        }
    }

    @Override
    public void onMessage(final Message message) {
        if (message instanceof TextMessage) {
            try {
                TextMessage textMessage = (TextMessage) message;
                LOG.infov("Received message from queue [{0}]: [{1}]", queueNameForIncomingMessages, textMessage.getText());
            } catch (JMSException e) {
                LOG.error("Failed to get text from message", e);
            }
        } else {
            LOG.infov("Received non-textMessage from queue: [{0}]", message);
        }
    }

    /**
     This method is called asynchronously by JMS when some error occurs.
     When using an asynchronous message listener it is recommended to use
     an exception listener also since JMS have no way to report errors
     otherwise.

     @param exception A JMS exception.
     */
    public void onException(JMSException exception) {
        LOG.error("Error occurred", exception);
    }
}
