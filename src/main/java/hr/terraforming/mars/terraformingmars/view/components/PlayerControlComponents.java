package hr.terraforming.mars.terraformingmars.view.components;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public record PlayerControlComponents(HBox playerListBar, Button passTurnButton,
                                      Button convertHeatButton, Button convertPlantsButton ) {
}