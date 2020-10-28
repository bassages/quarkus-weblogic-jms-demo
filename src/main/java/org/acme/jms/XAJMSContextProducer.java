package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import weblogic.transaction.InterposedTransactionManager;
import weblogic.transaction.TransactionHelper;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class XAJMSContextProducer {

    @Inject
    TransactionManager transactionManager;

    @ConfigProperty(name = "jmsConnectionFactoryName")
    String jmsConnectionFactoryName;
    @ConfigProperty(name = "jmsServerName")
    String jmsServerName;

    @Inject
    ContextFactory contextFactory;

    XAConnectionFactory connectionFactory;

    Map<Transaction, XAJMSContext> sessions = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            connectionFactory = (XAQueueConnectionFactory) contextFactory.get().lookup(jmsConnectionFactoryName);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public XAJMSContext getContext() {
        try {
            final Transaction transaction = transactionManager.getTransaction();

            if (!sessions.containsKey(transaction)) {
                final XAJMSContext xaJmsContext = connectionFactory.createXAContext();

                final InterposedTransactionManager itm = TransactionHelper.getClientInterposedTransactionManagerThrowsOnException(contextFactory.get(), jmsServerName);
                if (transaction == null) {
                    throw new RuntimeException("Please annotate your method with @Transactional");
                }

                // following will lead to java.lang.IllegalStateException: [JMSClientExceptions:055059]getXAResource can be called only from the server.
                // transaction.enlistResource(xaJmsContext.getXAResource());

                // instead, we need to get the XAResource from a InterposedTransactionManager
                // See https://docs.oracle.com/cd/E12839_01/web.1111/e13731/jtatximp.htm#WLJTA297
                // WebLogic Server can participate in distributed transactions coordinated by third-party systems (referred to as foreign transaction managers).
                // The WebLogic Server processing is done as part of the work of the external transaction.
                // The foreign transaction manager then drives the WebLogic Server transaction manager as part of its commit processing.
                // This is referred to as "importing" transactions into WebLogic Server.

                transaction.enlistResource(itm.getXAResource());

                sessions.put(transaction, xaJmsContext);
                return xaJmsContext;
            } else {
                return sessions.get(transaction);
            }

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
