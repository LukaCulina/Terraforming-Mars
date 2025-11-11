package hr.terraforming.mars.terraformingmars.enums;

import lombok.Getter;

public enum ConfigurationKey {

    RMI_HOST("rmi.host"), RMI_PORT("rmi.port");

    private final String key;

    private ConfigurationKey(String key) {
        this.key = key;
    }
}
