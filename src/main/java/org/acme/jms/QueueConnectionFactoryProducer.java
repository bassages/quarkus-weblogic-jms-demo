package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

@ApplicationScoped
public class QueueConnectionFactoryProducer {

    @ConfigProperty(name = "jmsConnectionFactoryName")
    String jmsConnectionFactoryName;

    @Inject
    Context context;

    @Produces
    public QueueConnectionFactory connectionFactory() throws NamingException {
        return (QueueConnectionFactory) context.lookup(jmsConnectionFactoryName);
    }
}
