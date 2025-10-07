package hr.terraforming.mars.terraformingmars;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import hr.terraforming.mars.terraformingmars.view.GameScreens;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TerraformingMarsApplication extends Application {
    @Override
   public void start(Stage stage) {

        ResourceConfig config = new ResourceConfig(
                "/hr/terraforming/mars/terraformingmars/",
                "/hr/terraforming/mars/terraformingmars/css/styles.css"
        );

        ScreenLoader.setConfig(config);

        stage.setTitle("Terraforming Mars");

        stage.setMaximized(true);
        stage.setResizable(true);
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());

        stage.setFullScreenExitHint("");

        GameScreens.setMainStage(stage);

        GameScreens.showChoosePlayersScreen();

        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}