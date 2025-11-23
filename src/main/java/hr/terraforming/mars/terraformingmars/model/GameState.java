package hr.terraforming.mars.terraformingmars.model;

import java.io.Serializable;

public record GameState(GameManager gameManager, GameBoard gameBoard) implements Serializable {

}