package hr.terraforming.mars.terraformingmars.network;

import java.io.Serializable;
import java.util.List;

public record CorporationOfferMessage(String playerName, List<String> corporationNames) implements Serializable {}
