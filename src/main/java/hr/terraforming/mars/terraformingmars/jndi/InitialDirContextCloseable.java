package hr.terraforming.mars.terraformingmars.jndi;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

public class InitialDirContextCloseable extends InitialDirContext implements AutoCloseable {

    private static final String CONFIGURATION_FILE_PROVIDER_URL = "file:D:/";

    public InitialDirContextCloseable() throws NamingException {
        addToEnvironment(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.fscontext.RefFSContextFactory");
        addToEnvironment(Context.PROVIDER_URL, CONFIGURATION_FILE_PROVIDER_URL);
    }
}
