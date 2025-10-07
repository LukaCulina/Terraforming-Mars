package hr.terraforming.mars.terraformingmars.view.components;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public record GlobalStatusComponents(ProgressBar oxygenProgressBar, Label oxygenLabel,
                                     ProgressBar temperatureProgressBar, Label temperatureLabel,
                                     Label oceansLabel,
                                     Label generationLabel, Label phaseLabel) {
}
