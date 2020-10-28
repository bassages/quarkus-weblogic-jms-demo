package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.XAJMSContext;
import javax.naming.Context;
import javax.transaction.Transactional;
import java.util.Random;

@ApplicationScoped
public class MessageSenderService {
    private static final Logger LOG = Logger.getLogger(MessageSenderService.class);

    @ConfigProperty(name = "queueNameForOutgoingMessages")
    String queueNameForOutgoingMessages;

    @Inject
    Context context;
    @Inject
    XAJMSContextProducer xajmsContextProducerProducer;

    private final Random random = new Random();

    @Transactional(rollbackOn = ForceRollbackException.class)
    public void send() throws ForceRollbackException {
        int randomNumber = random.nextInt(100);

        try {
            final XAJMSContext xaJmsContext = xajmsContextProducerProducer.getContext();
            final JMSProducer producer = xaJmsContext.getContext().createProducer();
            final Queue queue = (Queue) context.lookup(queueNameForOutgoingMessages);
            final TextMessage textMessage = xaJmsContext.createTextMessage(Integer.toString(randomNumber));

            LOG.infov("Sending message to queue [{0}]: [{1}]", queueNameForOutgoingMessages, textMessage.getText());
            producer.send(queue, textMessage);

        } catch (Exception e) {
            LOG.error("Failed to send message to queue", e);
        }
        if (randomNumber % 2 == 0) {
            LOG.info("Don't send even numbers, trigger rollback!");
            throw new ForceRollbackException();
        }
    }
}
