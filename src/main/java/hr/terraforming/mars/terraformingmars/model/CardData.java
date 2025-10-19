package hr.terraforming.mars.terraformingmars.model;

import java.util.List;
import java.util.Map;

public record CardData(
        String name,
        int cost,
        List<String> tags,
        String description,
        int victoryPoints,
        List<Map<String, Object>> effects,
        List<Map<String, Object>> requirements,
        String tileToPlace
) { }
