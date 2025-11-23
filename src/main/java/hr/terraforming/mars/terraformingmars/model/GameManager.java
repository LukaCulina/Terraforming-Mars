package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.util.GamePhaseUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameManager implements Serializable {
    
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    @Getter private GamePhase currentPhase;
    @Getter private int generation = 1;
    private final List<Player> passedPlayers = new ArrayList<>();
    private transient GameBoard board;
    private int cardDraftPlayerIndex = 0;
    @Getter private int actionsTakenThisTurn = 0;
    private final List<Corporation> remainingCorporations;
    private final List<Card> remainingCards;

    public GameManager(List<Player> players, GameBoard gameBoard) {
        this.players = new ArrayList<>(players);
        this.remainingCorporations = new ArrayList<>(CorporationFactory.getAllCorporations());
        this.remainingCards = new ArrayList<>(CardFactory.getAllCards());
        relink(gameBoard);
    }

    public void relink(GameBoard gameBoard) {
        this.board = gameBoard;
        for (Player p : this.players) {
            p.setBoard(gameBoard);
        }
    }

    public List<Corporation> getCorporationOffer() {
        List<Corporation> shuffledCorps = new ArrayList<>(remainingCorporations);
        Collections.shuffle(shuffledCorps);
        int count = Math.min(2, shuffledCorps.size());
        List<Corporation> offer = new ArrayList<>(shuffledCorps.subList(0, count));
        this.remainingCorporations.removeAll(offer);
        return offer;
    }

    public boolean assignCorporationAndAdvance(Corporation chosenCorp) {
        getCurrentPlayer().setCorporation(chosenCorp);
        log.info("Player {} chose corporation: {}", getCurrentPlayer().getName(), chosenCorp.name());
        this.currentPlayerIndex++;

        if (currentPlayerIndex >= players.size()) {
            this.currentPlayerIndex = 0;
            return false;
        }
        return true;
    }

    public Player getCurrentPlayerForDraft() {
        if (cardDraftPlayerIndex < players.size()) {
            return players.get(cardDraftPlayerIndex);
        }
        return null;
    }

    public boolean advanceDraftPlayer() {
        cardDraftPlayerIndex++;
        return cardDraftPlayerIndex < players.size();
    }

    public List<Card> drawCards(int count) {
        if (remainingCards.isEmpty()) {
            log.warn("There are no more cards in the draw pile.");
            return new ArrayList<>();
        }

        List<Card> shuffledCards = new ArrayList<>(remainingCards);
        Collections.shuffle(shuffledCards);
        int cardsToTake = Math.min(count, shuffledCards.size());
        List<Card> drawnCards = new ArrayList<>(shuffledCards.subList(0, cardsToTake));
        this.remainingCards.removeAll(drawnCards);

        log.info("Drawn {} cards. Cards remaining in deck: {}.", drawnCards.size(), this.remainingCards.size());
        return drawnCards;
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

    private void nextPlayer() {
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
            nextPlayer();
            return false;
        }
    }

    public void doProduction() {
        GamePhaseUtils.executeProduction(players);
    }

    public void startNewGeneration() {
        generation++;
        currentPhase = GamePhase.RESEARCH;
        currentPlayerIndex = 0;
        passedPlayers.clear();
        log.info("Generation {} begins. Current Phase: {}", generation, currentPhase);
    }

    public void beginActionPhase() {
        this.currentPhase = GamePhase.ACTIONS;
        log.info("Phase: {}", this.currentPhase);
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }

    public GameBoard getGameBoard() {
        return board;
    }

    public List<Player> calculateFinalScores() {
        return GamePhaseUtils.calculateFinalScores(players);
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
        this.remainingCorporations.clear();
        this.remainingCorporations.addAll(CorporationFactory.getAllCorporations());
        this.remainingCards.clear();
        this.remainingCards.addAll(CardFactory.getAllCards());

        log.info("GameManager has been reset for a new game.");
    }

    public void setCurrentPlayerByName(String playerName) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(playerName)) {
                this.currentPlayerIndex = i;
                return;
            }
        }
    }

    public Player getPlayerByName(String playerName) {
        for (Player player : players) {
            if (player.getName().equals(playerName)) {
                return player;
            }
        }
        log.warn("Player with name '{}' not found!", playerName);
        return null;
    }
}