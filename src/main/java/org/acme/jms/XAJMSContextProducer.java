package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import weblogic.transaction.ClientTxHelper;
import weblogic.transaction.InterposedTransactionManager;
import weblogic.transaction.TransactionHelper;
import weblogic.transaction.TxHelperService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
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

    @Inject
    Context context;

    XAConnectionFactory connectionFactory;

    Map<Transaction, XAJMSContext> sessions = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            connectionFactory = (XAQueueConnectionFactory) context.lookup(jmsConnectionFactoryName);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public XAJMSContext getContext() {
        try {
            final Transaction transaction = transactionManager.getTransaction();

            if (!sessions.containsKey(transaction)) {
                final XAJMSContext xaJmsContext = connectionFactory.createXAContext();

                // See https://docs.oracle.com/cd/E12839_01/web.1111/e13731/jtatximp.htm#WLJTA297
                // WebLogic Server can participate in distributed transactions coordinated by third-party systems (referred to as foreign transaction managers).
                // The WebLogic Server processing is done as part of the work of the external transaction.
                // The foreign transaction manager then drives the WebLogic Server transaction manager as part of its commit processing.
                // This is referred to as "importing" transactions into WebLogic Server.

                final String serverName = "AdminServer"; // Can be found in Weblogic console: Environment -> Severs -> <select server> -> Configuration -> General->View JNDI tree
                final InterposedTransactionManager itm = TransactionHelper.getClientInterposedTransactionManagerThrowsOnException(context, serverName);
                if (transaction == null) {
                    throw new RuntimeException("Please annotate your method with @Transactional");
                }
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
