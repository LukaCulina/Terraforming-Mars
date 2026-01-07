package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import hr.terraforming.mars.terraformingmars.service.DeckService;
import hr.terraforming.mars.terraformingmars.service.ProductionService;
import hr.terraforming.mars.terraformingmars.service.ScoringService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameManager implements Serializable {

    private final List<Player> players;
    private int currentPlayerIndex = 0;
    @Setter @Getter private GamePhase currentPhase;
    @Getter private int generation = 0;
    private final List<Player> passedPlayers = new ArrayList<>();
    private transient GameBoard board;
    private int cardDraftPlayerIndex = 0;
    @Getter private int actionsTakenThisTurn = 0;
    private final DeckService deckService;
    @Getter private Player firstPlayer;

    public GameManager(List<Player> players, GameBoard gameBoard) {
        this.players = new ArrayList<>(players);
        this.deckService = new DeckService();
        this.firstPlayer = players.isEmpty() ? null : players.getFirst();
        relink(gameBoard);
    }

    public void relink(GameBoard gameBoard) {
        this.board = gameBoard;
        for (Player p : this.players) {
            p.setBoard(gameBoard);
        }
    }

    public void shuffleCorporations() {
        deckService.shuffleCorporations();
    }

    public void shuffleCards() {
        deckService.shuffleCards();
    }

    public List<Corporation> drawCorporations(int count) {
        return deckService.drawCorporations(count);
    }

    public List<Corporation> getCorporationOffer() {
        return deckService.getInitialCorporations();
    }

    public List<Card> drawCards(int count) {
        return deckService.drawCards(count);
    }

    public void assignCorporationAndAdvance(Corporation chosenCorp) {
        getCurrentPlayer().setCorporation(chosenCorp);
        this.currentPlayerIndex++;

        if (currentPlayerIndex >= players.size()) {
            this.currentPlayerIndex = 0;
        }
    }

    public Player getCurrentDraftPlayer() {
        if (cardDraftPlayerIndex >= players.size()) {
            return null;
        }

        return players.get(cardDraftPlayerIndex);
    }

    public boolean hasMoreDraftPlayers() {
        cardDraftPlayerIndex++;
        return cardDraftPlayerIndex < players.size();

    }

    public void resetDraftPhase() {
        this.cardDraftPlayerIndex = 0;
    }

    public void incrementActionsTaken() {
        this.actionsTakenThisTurn++;
    }

    public void resetActionsTaken() {
        this.actionsTakenThisTurn = 0;
    }

    public void startGame() {
        this.currentPhase = GamePhase.ACTIONS;
        this.generation = 1;
        this.currentPlayerIndex = 0;
        this.passedPlayers.clear();
        resetActionsTaken();
        log.info("Starting Generation {}. Phase: {}", generation, currentPhase);
    }

    private void advanceToNextPlayer() {
        if (passedPlayers.size() < players.size()) {
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } while (passedPlayers.contains(getCurrentPlayer()));
        }
    }

    public boolean passTurn() {
        Player p = getCurrentPlayer();
        if (!passedPlayers.contains(p)) {
            passedPlayers.add(p);
            log.info("{} has passed.", p.getName());
        }
        resetActionsTaken();
        if (passedPlayers.size() >= players.size()) {
            return true;
        } else {
            advanceToNextPlayer();
            return false;
        }
    }

    public void doProduction() {
        ProductionService.executeProduction(players);
    }

    public void startNewGeneration() {
        generation++;
        currentPhase = GamePhase.RESEARCH;
        currentPlayerIndex = 0;
        this.actionsTakenThisTurn = 0;
        passedPlayers.clear();
    }

    public void beginActionPhase() {
        this.currentPhase = GamePhase.ACTIONS;
        if (firstPlayer != null) {
            this.currentPlayerIndex = players.indexOf(firstPlayer);
            log.info("Action phase beginning. Starting player: {}", getCurrentPlayer().getName());
        } else {
            this.currentPlayerIndex = 0;
            log.warn("firstPlayer is null, defaulting to index 0");
        }        this.actionsTakenThisTurn = 0;
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }

    public GameBoard getGameBoard() { return board; }

    public List<Player> calculateFinalScores() {
        return ScoringService.calculateFinalScores(players);
    }

    public void resetForNewGame(GameBoard newBoard) {
        this.board = newBoard;
        for (Player p : this.players) {
            p.setBoard(newBoard);
        }

        this.generation = 1;
        this.currentPhase = GamePhase.ACTIONS;
        this.currentPlayerIndex = 0;
        this.actionsTakenThisTurn = 0;
        this.passedPlayers.clear();
        this.cardDraftPlayerIndex = 0;
        deckService.reset();
    }

    public void setCurrentPlayerByName(String playerName) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(playerName)) {
                this.currentPlayerIndex = i;
                return;
            }
        }
        log.warn("Player '{}' not found", playerName);
    }

    public Player getPlayerByName(String playerName) {
        return players.stream()
                .filter(p -> p.getName().equals(playerName))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Player '{}' not found!", playerName);
                    return null;
                });
    }

    public void rotateFirstPlayer() {
        if (players.isEmpty()) {
            log.warn("Cannot rotate first player - no players in game!");
            return;
        }

        int currentIndex = players.indexOf(firstPlayer);
        int nextIndex = (currentIndex + 1) % players.size();
        this.firstPlayer = players.get(nextIndex);

        log.info("First player rotated to: {}", firstPlayer.getName());
    }
}