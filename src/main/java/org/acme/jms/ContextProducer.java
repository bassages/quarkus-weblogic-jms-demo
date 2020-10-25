package org.acme.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class ContextProducer {

    private static final String WEBLOGIC_JNDI_WLINITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";

    @ConfigProperty(name = "jmsProviderUrl")
    String jmsProviderUrl;
    @ConfigProperty(name = "jmsProviderSecurityCredentials")
    String jmsProviderSecurityCredentials;
    @ConfigProperty(name = "jmsProviderSecurityPrincipal")
    String jmsProviderSecurityPrincipal;

    @Produces
    public Context produceContext() throws NamingException {
        final Context context = new InitialContext();
        context.addToEnvironment(Context.INITIAL_CONTEXT_FACTORY, WEBLOGIC_JNDI_WLINITIAL_CONTEXT_FACTORY);
        context.addToEnvironment(Context.PROVIDER_URL,jmsProviderUrl);
        context.addToEnvironment(Context.SECURITY_CREDENTIALS,jmsProviderSecurityCredentials);
        context.addToEnvironment(Context.SECURITY_PRINCIPAL,jmsProviderSecurityPrincipal);
        return context;
    }
}
