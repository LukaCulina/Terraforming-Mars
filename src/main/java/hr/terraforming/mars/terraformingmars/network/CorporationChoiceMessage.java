package hr.terraforming.mars.terraformingmars.network;

import java.io.Serializable;

public record CorporationChoiceMessage(String corporationName) implements Serializable {}
