package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serializable;
import java.util.List;

public record CardChoiceMessage(List<String> cardNames) implements Serializable {}
