package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Corporation;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class DeckService implements Serializable {

    private final List<Corporation> remainingCorporations;
    private final List<Card> remainingCards;

    public DeckService() {
        this.remainingCorporations = new ArrayList<>(CorporationFactory.getAllCorporations());
        this.remainingCards = new ArrayList<>(CardFactory.getAllCards());
    }

    public void shuffleCorporations() {
        Collections.shuffle(remainingCorporations);
        log.info("Corporations shuffled. Remaining: {}", remainingCorporations.size());
    }

    public void shuffleCards() {
        Collections.shuffle(remainingCards);
        log.info("Project cards shuffled. Remaining: {}", remainingCards.size());
    }

    public List<Corporation> drawCorporations(int count) {
        if (remainingCorporations.isEmpty()) {
            log.warn("No more corporations in the deck!");
            return new ArrayList<>();
        }

        int toTake = Math.min(count, remainingCorporations.size());
        List<Corporation> drawn = new ArrayList<>(remainingCorporations.subList(0, toTake));

        remainingCorporations.removeAll(drawn);

        return drawn;
    }

    public List<Corporation> getCorporationOffer() {
        return drawCorporations(2);
    }

    public List<Card> drawCards(int count) {
        if (remainingCards.isEmpty()) {
            log.warn("There are no more cards in the draw pile.");
            return new ArrayList<>();
        }

        int cardsToTake = Math.min(count, remainingCards.size());
        List<Card> drawnCards = new ArrayList<>(remainingCards.subList(0, cardsToTake));
        remainingCards.removeAll(drawnCards);

        return drawnCards;
    }

    public void reset() {
        remainingCorporations.clear();
        remainingCorporations.addAll(CorporationFactory.getAllCorporations());
        remainingCards.clear();
        remainingCards.addAll(CardFactory.getAllCards());
    }
}
