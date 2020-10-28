package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.NamingException;

@ApplicationScoped
public class QueueConnectionFactoryProducer {

    @ConfigProperty(name = "jmsConnectionFactoryName")
    String jmsConnectionFactoryName;

    @Inject
    ContextFactory contextFactory;

    @Produces
    public XAQueueConnectionFactory xaQueueConnectionFactory() throws NamingException {
        return (XAQueueConnectionFactory) contextFactory.get().lookup(jmsConnectionFactoryName);
    }
}
