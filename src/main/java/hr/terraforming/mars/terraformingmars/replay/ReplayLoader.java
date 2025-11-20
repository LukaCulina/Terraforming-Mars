package hr.terraforming.mars.terraformingmars.replay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.scene.control.Alert;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplayLoader {

    private static final Type REPLAY_DATA_TYPE = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
    private static final Gson GSON = new Gson();

    public List<GameMove> loadMoves() {
        return XmlUtils.readGameMoves();
    }

    public void setupInitialState(List<GameMove> moves, GameManager gameManager) {
        GameMove initialSetupMove = moves.stream()
                .filter(m -> m.actionType() == ActionType.INITIAL_SETUP)
                .findFirst()
                .orElse(null);

        if (initialSetupMove != null) {
            setupStateFromDetails(gameManager, initialSetupMove.details());
            moves.remove(initialSetupMove);
        } else {
            new Alert(Alert.AlertType.WARNING, "Replay file is missing initial setup data! Cards will not be shown.").show();
        }
    }

    public void updatePlayerHandsFromDetails(GameManager gameManager, String jsonDetails) {
        setupStateFromDetails(gameManager, jsonDetails);
    }

    private void setupStateFromDetails(GameManager gameManager, String jsonDetails) {
        Map<String, Map<String, Object>> dataMap = GSON.fromJson(jsonDetails, REPLAY_DATA_TYPE);

        for (Player player : gameManager.getPlayers()) {
            Map<String, Object> playerData = dataMap.get(player.getName());
            if (playerData == null) {
                continue;
            }

            handlePlayerCorporation(player, playerData);
            handlePlayerHand(player, playerData);
        }
    }

    private void handlePlayerCorporation(Player player, Map<String, Object> playerData) {
        if (!playerData.containsKey("corporation")) {
            return;
        }

        String corpName = (String) playerData.get("corporation");
        Corporation corp = CorporationFactory.getCorporationByName(corpName);
        if (corp != null) {
            player.setCorporation(corp);
        }
        player.resetForNewGame();
    }

    private void handlePlayerHand(Player player, Map<String, Object> playerData) {
        if (!playerData.containsKey("hand")) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> newCardNames = (List<String>) playerData.get("hand");
        if (newCardNames == null) {
            return;
        }

        List<Card> oldHand = new ArrayList<>(player.getHand());
        List<Card> newHand = newCardNames.stream().map(CardFactory::getCardByName).toList();

        List<Card> boughtCards = new ArrayList<>(newHand);
        boughtCards.removeAll(oldHand);
        player.spendMC(boughtCards.size() * 3);

        player.getHand().clear();
        player.getHand().addAll(newHand);
    }
}
