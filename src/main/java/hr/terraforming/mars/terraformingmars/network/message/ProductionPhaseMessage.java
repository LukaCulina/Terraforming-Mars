package hr.terraforming.mars.terraformingmars.network.message;

import hr.terraforming.mars.terraformingmars.model.ProductionReport;

import java.io.Serializable;
import java.util.List;

public record ProductionPhaseMessage(List<ProductionReport> summaries, int generation) implements Serializable { }
