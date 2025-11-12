package hr.terraforming.mars.terraformingmars.jndi;

import hr.terraforming.mars.terraformingmars.exception.ConfigurationException;

import javax.naming.NamingException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationReader {
    private static final String CONFIGURATION_FILE_NAME = "conf/application.properties";

    private ConfigurationReader() {}

    public static String getStringValue(ConfigurationKey key) {
        try (InitialDirContextCloseable context = new InitialDirContextCloseable()){
            Object object = context.lookup(CONFIGURATION_FILE_NAME);
            Properties props = new Properties();
            try(FileReader reader = new FileReader(object.toString())) {
                props.load(reader);
            }
            return props.getProperty(key.getKey());
        } catch (NamingException | IOException e) {
            throw new ConfigurationException("Failed to read the configuration key " + key.getKey(), e);
        }
    }

    public static Integer getIntegerValue(ConfigurationKey key) {
        return Integer.valueOf(getStringValue(key));
    }
}