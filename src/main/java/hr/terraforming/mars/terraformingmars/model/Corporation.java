package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;

import java.io.Serializable;
import java.util.Map;

public record Corporation(String name, int startingMC, Map<ResourceType, Integer> startingResources,
                          Map<ResourceType, Integer> startingProduction, String abilityDescription) implements Serializable {
}

