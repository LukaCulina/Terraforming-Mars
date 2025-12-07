package hr.terraforming.mars.terraformingmars.view.component;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public record PlayerControlComponents(HBox playerListBar, Button passTurnButton,
                                      Button convertHeatButton, Button convertPlantsButton ) {
}