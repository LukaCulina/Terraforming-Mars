package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.exception.GameStateException;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TurnManager implements Serializable {
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private final List<Player> passedPlayers = new ArrayList<>();
    @Getter
    private Player firstPlayer;
    @Getter
    private int actionsTakenThisTurn = 0;

    public TurnManager(List<Player> players) {
        this.players = players;
        this.firstPlayer = players.isEmpty() ? null : players.getFirst();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void incrementActionsTaken() {
        actionsTakenThisTurn++;
    }

    public void resetActionsTaken() {
        actionsTakenThisTurn = 0;
    }

    public void advanceCorporationDraft() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }
    }

    public boolean passTurn() {
        Player currentPlayer = getCurrentPlayer();

        if (!passedPlayers.contains(currentPlayer)) {
            passedPlayers.add(currentPlayer);
        }

        resetActionsTaken();

        if (passedPlayers.size() >= players.size()) {
            return true;
        } else {
            advanceToNextPlayer();
            return false;
        }
    }

    private void advanceToNextPlayer() {
        if (passedPlayers.size() < players.size()) {
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } while (passedPlayers.contains(getCurrentPlayer()));
        }
    }

    public void rotateFirstPlayer() {
        if (players.isEmpty()) {
            log.warn("Cannot rotate first player - no players in game!");
            return;
        }

        int currentIndex = players.indexOf(firstPlayer);
        int nextIndex = (currentIndex + 1) % players.size();
        firstPlayer = players.get(nextIndex);

        log.info("First player rotated to: {}", firstPlayer.getName());
    }

    public void setCurrentPlayerByName(String playerName) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(playerName)) {
                currentPlayerIndex = i;
                return;
            }
        }
        throw new GameStateException("Cannot set current player: player '" + playerName + "' not found");
    }

    public void beginActionPhase() {
        if (firstPlayer != null) {
            currentPlayerIndex = players.indexOf(firstPlayer);
            log.info("Action phase beginning. Starting player: {}", getCurrentPlayer().getName());
        } else {
            currentPlayerIndex = 0;
            log.warn("firstPlayer is null, defaulting to index 0");
        }
        actionsTakenThisTurn = 0;
    }

    public void reset() {
        currentPlayerIndex = 0;
        actionsTakenThisTurn = 0;
        passedPlayers.clear();
    }
}
