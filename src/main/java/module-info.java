module hr.terraforming.mars.terraformingmars {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.slf4j;
    requires com.google.gson;

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
    exports hr.terraforming.mars.terraformingmars.view.components;
    exports hr.terraforming.mars.terraformingmars.service;
    exports hr.terraforming.mars.terraformingmars.util;
    exports hr.terraforming.mars.terraformingmars.config;
    exports hr.terraforming.mars.terraformingmars.model.effects;
    exports hr.terraforming.mars.terraformingmars.replay;
    opens hr.terraforming.mars.terraformingmars.replay to javafx.fxml;
}