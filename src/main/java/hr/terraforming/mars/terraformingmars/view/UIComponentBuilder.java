package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public record UIComponentBuilder(TerraformingMarsController controller, ActionManager actionManager,
                                 GameManager gameManager) {

    public void createPlayerButtons(HBox playerListBar) {
        playerListBar.getChildren().clear();
        for (Player player : gameManager.getPlayers()) {
            Button btn = new Button(player.getName());
            btn.getStyleClass().add("player-select-button");
            btn.setOnAction(_ -> controller.showPlayerBoard(player));
            playerListBar.getChildren().add(btn);
        }
    }

    public void createMilestoneButtons(VBox milestonesBox) {
        milestonesBox.getChildren().clear();
        Label milestonesLabel = new Label("Milestones");
        milestonesLabel.getStyleClass().add("project-milestone");
        milestonesBox.getChildren().add(milestonesLabel);

        for (Milestone milestone : Milestone.values()) {
            Button btn = new Button(milestone.getName());
            btn.prefWidthProperty().bind(milestonesBox.widthProperty().multiply(0.5));
            btn.getStyleClass().add("milestone-button");
            btn.setUserData(milestone);
            btn.setOnAction(_ -> actionManager.handleClaimMilestone(milestone));

            Tooltip tooltip = new Tooltip(milestone.getDescription() + "\n(Price: 8 MC)");
            tooltip.setStyle("-fx-font-size: 14px;");

            btn.setTooltip(tooltip);
            milestonesBox.getChildren().add(btn);
        }
    }

    public void createStandardProjectButtons(VBox standardProjectsBox) {
        standardProjectsBox.getChildren().clear();
        Label projectsLabel = new Label("Standard \nProjects ");
        projectsLabel.getStyleClass().add("project-milestone");
        standardProjectsBox.getChildren().add(projectsLabel);

        for (StandardProject project : StandardProject.values()) {
            Button btn = new Button(project.getName());
            btn.prefWidthProperty().bind(standardProjectsBox.widthProperty().multiply(0.8));
            btn.getStyleClass().add("project-button");
            btn.setUserData(project);

            Text icon = new Text(project.getIcon());
            icon.setStyle("-fx-font-size: 20px;");
            btn.setGraphic(icon);
            Tooltip tooltip = new Tooltip(project.getDescription() + "\n(Price: " + project.getCost() + " MC)");
            tooltip.setStyle("-fx-font-size: 14px;");
            btn.setTooltip(tooltip);

            btn.setOnAction(e -> actionManager.handleStandardProject((StandardProject) ((Button) e.getSource()).getUserData()));
            standardProjectsBox.getChildren().add(btn);
        }
    }
}
