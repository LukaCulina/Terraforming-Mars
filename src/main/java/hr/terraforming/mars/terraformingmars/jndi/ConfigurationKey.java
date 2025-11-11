package hr.terraforming.mars.terraformingmars.jndi;

import lombok.Getter;

@Getter
public enum ConfigurationKey {

    RMI_PORT("rmi.port"), HOSTNAME("hostname");

    private final String key;

    ConfigurationKey(String key) {
        this.key = key;
    }
}
