package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serializable;

public record CorporationChoiceMessage(String corporationName) implements Serializable {}
