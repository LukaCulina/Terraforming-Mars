package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.TerraformingMarsController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenLoader;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ClientResearchCoordinator {

    private final TerraformingMarsController controller;
    private boolean isModalOpen = false;

    public ClientResearchCoordinator(TerraformingMarsController controller) {
        this.controller = controller;
    }

    public void checkAndHandle() {
        GameManager gameManager = controller.getGameManager();

        if (gameManager.getCurrentPhase() != GamePhase.RESEARCH) {
            isModalOpen = false;
            return;
        }

        Player currentResearchPlayer = gameManager.getCurrentPlayerForDraft();
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        log.debug("🔍 CLIENT Research Check: current={}, me={}",
                (currentResearchPlayer != null ? currentResearchPlayer.getName() : "NULL"),
                myPlayerName);

        if (playerType == PlayerType.CLIENT
                && currentResearchPlayer != null
                && Objects.equals(myPlayerName, currentResearchPlayer.getName())) {

            log.info("Opening research modal for CLIENT player '{}'", currentResearchPlayer.getName());

            if (!isModalOpen) {
                isModalOpen = true;
                Platform.runLater(() -> openModal(currentResearchPlayer, gameManager));
            }
        }
    }

    private void openModal(Player player, GameManager gameManager) {
        List<Card> offer = gameManager.drawCards(4);

        ScreenLoader.showAsModal(
                controller.getSceneWindow(),
                "ChooseCards.fxml",
                "Research Phase - " + player.getName(),
                0.7, 0.8,
                (ChooseCardsController c) -> c.setup(
                        player,
                        offer,
                        boughtCards -> finishResearch(player, boughtCards),
                        gameManager,
                        true
                )
        );
    }

    private void finishResearch(Player player, List<Card> boughtCards) {
        int cost = boughtCards.size() * 3;
        if (player.spendMC(cost)) {
            player.getHand().addAll(boughtCards);
        }

        if (!boughtCards.isEmpty()) {
            String details = boughtCards.stream()
                    .map(Card::getName)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            GameMove move = new GameMove(
                    player.getName(),
                    ActionType.OPEN_CHOOSE_CARDS_MODAL,
                    details,
                    LocalDateTime.now()
            );
            controller.getActionManager().recordAndSaveMove(move);
        }

        isModalOpen = false;
    }
}