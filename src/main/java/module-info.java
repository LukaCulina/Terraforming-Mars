module hr.terraforming.mars.terraformingmars {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.slf4j;

    opens hr.terraforming.mars.terraformingmars to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars;
    exports hr.terraforming.mars.terraformingmars.model;
    opens hr.terraforming.mars.terraformingmars.model to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars.factory;
    opens hr.terraforming.mars.terraformingmars.factory to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars.enums;
    opens hr.terraforming.mars.terraformingmars.enums to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars.controller;
    opens hr.terraforming.mars.terraformingmars.controller to javafx.fxml;
    opens hr.terraforming.mars.terraformingmars.view to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars.view;
    opens hr.terraforming.mars.terraformingmars.manager to javafx.fxml;
    exports hr.terraforming.mars.terraformingmars.manager;
}