package hr.terraforming.mars.terraformingmars.network.message;

import java.io.Serial;
import java.io.Serializable;

public record GameOverMessage() implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}