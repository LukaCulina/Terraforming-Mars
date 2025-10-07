package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameManager implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private GamePhase currentPhase;
    private int generation = 1;
    private final List<Player> passedPlayers = new ArrayList<>();
    private transient GameBoard board;
    private int cardDraftPlayerIndex = 0;
    private int actionsTakenThisTurn = 0;
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
        logger.info("Player {} chose corporation: {}", getCurrentPlayer().getName(), chosenCorp.name());

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
            logger.warn("There are no more cards in the draw pile.");
            return new ArrayList<>();
        }

        List<Card> shuffledCards = new ArrayList<>(remainingCards);
        Collections.shuffle(shuffledCards);

        int cardsToTake = Math.min(count, shuffledCards.size());
        List<Card> drawnCards = new ArrayList<>(shuffledCards.subList(0, cardsToTake));

        this.remainingCards.removeAll(drawnCards);

        logger.info("Drawn {} cards. Cards remaining in deck: {}.", drawnCards.size(), this.remainingCards.size());
        return drawnCards;
    }

    public int getActionsTakenThisTurn() {
        return actionsTakenThisTurn;
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

        logger.info("Starting Generation {}. Phase: {}", generation, currentPhase);
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

            logger.info("{} has passed.", p.getName());
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
        logger.info("Production Phase Started");

        for (Player p : players) {
            int income = p.getTR() + p.productionProperty(ResourceType.MEGACREDITS).get();
            p.addMC(income);
            int energy = p.resourceProperty(ResourceType.ENERGY).get();
            p.addResource(ResourceType.HEAT, energy);
            p.resourceProperty(ResourceType.ENERGY).set(0);

            p.getProductionMap().forEach((type, amount) -> {
                if (type != ResourceType.MEGACREDITS) {
                    p.addResource(type, amount.get());
                }
            });
            logger.info("Player {} energy set to 0. Current value is: {}",
                    p.getName(), p.resourceProperty(ResourceType.ENERGY).get());
        }
        logger.info("Production Finished");
    }

    public void startNewGeneration() {
        generation++;
        currentPhase = GamePhase.RESEARCH;
        currentPlayerIndex = 0;
        passedPlayers.clear();
        logger.info("Generation {} begins. Current Phase: {}", generation, currentPhase);
    }

    public void beginActionPhase() {
        this.currentPhase = GamePhase.ACTIONS;
        logger.info("Phase: {}", this.currentPhase);
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public int getGeneration() { return generation; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public GameBoard getGameBoard() {
        return board;
    }

    public List<Player> calculateFinalScores() {
        logger.info(" FINAL SCORING");
        for (Player p : players) {
            p.calculateTilePoints();
            logger.info("{} - Final Score: {}", p.getName(), p.getFinalScore());

        }

        players.sort(Comparator.comparingInt(Player::getFinalScore).reversed()
                .thenComparing(Player::getMC, Comparator.reverseOrder()));

        return new ArrayList<>(players);
    }
}
