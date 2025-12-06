package hr.terraforming.mars.terraformingmars.network;

import java.io.Serializable;
import java.util.List;

public record InitialCardsOfferMessage(String playerName, List<String> cardNames) implements Serializable {}
