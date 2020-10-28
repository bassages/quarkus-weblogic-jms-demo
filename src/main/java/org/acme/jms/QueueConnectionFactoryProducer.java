package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

@ApplicationScoped
public class QueueConnectionFactoryProducer {

    @ConfigProperty(name = "jmsConnectionFactoryName")
    String jmsConnectionFactoryName;

    @Inject
    Context context;

    @Produces
    public XAQueueConnectionFactory xaQueueConnectionFactory() throws NamingException {
        return (XAQueueConnectionFactory) context.lookup(jmsConnectionFactoryName);
    }
}
