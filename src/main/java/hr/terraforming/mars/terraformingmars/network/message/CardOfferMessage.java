package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serializable;
import java.util.List;

public record CardOfferMessage(String playerName, List<String> cardNames) implements Serializable {}
