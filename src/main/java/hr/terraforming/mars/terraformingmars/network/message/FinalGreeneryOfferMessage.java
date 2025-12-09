package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serializable;

public record FinalGreeneryOfferMessage(
        String playerName
) implements Serializable {}
