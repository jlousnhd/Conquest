package conquest;

import java.util.*;

public final class PlayerData {
	
	public static final int MAX_NAME_LENGTH = 16;
	
	private int freeArmies;
	private long ownedTerritories;
	private long ownedCards;
	private final Integer id;
	
	public PlayerData(Integer id) {
		
		if(id == null)
			throw new NullPointerException();
		
		this.id = id;
		
	}
	
	public Integer getID() {
		return id;
	}
	
	public boolean isSpecificCardsKnown() {
		return ownedCards >= 0L;
	}
	
	public boolean holdsCards(long mask) {
		
		if(!isSpecificCardsKnown())
			throw new IllegalStateException();
		
		return (ownedCards & mask) == mask;
		
	}
	
	public boolean holdsCards(EnumSet<Card> cards) {
		return holdsCards(Card.setToMask(cards));
	}
	
	public boolean holdsCard(Card card) {
		return holdsCards(card.MASK);
	}
	
	public void setOwnedCards(long mask) {
		
		if((mask & ~Card.ALL_CARDS_MASK) != 0L)
			throw new IllegalArgumentException();
		
		this.ownedCards = mask;
		
	}
	
	public void setOwnedCards(EnumSet<Card> cards) {
		this.ownedCards = Card.setToMask(cards);
	}
	
	public long getOwnedCardsAsMask() {
		
		if(!isSpecificCardsKnown())
			throw new IllegalStateException();
		
		return ownedCards;
		
	}
	
	public EnumSet<Card> getOwnedCards() {
		return Card.maskToSet(ownedCards);
	}
	
	public void giveCard(Card card) {
		
		if(!isSpecificCardsKnown())
			throw new IllegalStateException();
		
		if(holdsCard(card))
			throw new IllegalArgumentException();
		
		ownedCards |= card.MASK;
		
	}
	
	public void takeCard(Card card) {
		
		if(!holdsCard(card))
			throw new IllegalArgumentException();
		
		ownedCards &= ~card.MASK;
		
	}
	
	public void takeCards(long cards) {
		
		if(!holdsCards(cards))
			throw new IllegalArgumentException();
		
		ownedCards &= ~cards;
		
	}
	
	public void takeCards(EnumSet<Card> cards) {
		takeCards(Card.setToMask(cards));
	}
	
	public void setCardCount(int count) {
		
		if(count < 0 || count > Card.CARD_COUNT)
			throw new IllegalArgumentException();
		
		ownedCards = -count;
		
	}
	
	public int cardCount() {
		
		if(!isSpecificCardsKnown())
			return (int) -ownedCards;
		
		return Util.countBits(ownedCards);
		
	}
	
	public boolean holdsTerritories(long mask) {
		return (ownedTerritories & mask) == mask;
	}
	
	public boolean holdsTerritories(EnumSet<Territory> territories) {
		return holdsTerritories(Territory.setToMask(territories));
	}
	
	public boolean holdsTerritory(Territory territory) {
		return holdsTerritories(territory.MASK);
	}
	
	public boolean holdsContinent(Continent continent) {
		return holdsTerritories(continent.MASK);
	}
	
	public void setOwnedTerritories(long mask) {
		this.ownedTerritories = mask & Territory.ALL_TERRITORIES_MASK;
	}
	
	public void setOwnedTerritories(EnumSet<Territory> territories) {
		this.ownedTerritories = Territory.setToMask(territories);
	}
	
	public long getOwnedTerritoriesAsMask() {
		return ownedTerritories;
	}
	
	public EnumSet<Territory> getOwnedTerritories() {
		return Territory.maskToSet(ownedTerritories);
	}
	
	public void giveTerritory(Territory territory) {
		
		if(holdsTerritory(territory))
			throw new IllegalArgumentException();
		
		ownedTerritories |= territory.MASK;
		
	}
	
	public void takeTerritory(Territory territory) {
		
		if(!holdsTerritory(territory))
			throw new IllegalArgumentException();
		
		ownedTerritories &= ~territory.MASK;
		
	}
	
	public int territoryCount() {
		return Util.countBits(ownedTerritories);
	}
	
	private int calculateTerritoryIncome() {
		
		// Calculate number of armies
		int armies = territoryCount() / 3;
		
		return armies < 3 ? 3 : armies;
		
	}
	
	private int calculateContinentIncome() {
		
		int total = 0;
		
		for(Continent continent : Continent.VALUES)
			total += holdsContinent(continent) ? continent.ARMY_BONUS : 0;
		
		return total;
		
	}
	
	public int calculateIncome() {
		return calculateTerritoryIncome() + calculateContinentIncome();
	}
	
	public void giveFreeArmies(int armies) {
		
		if(this.freeArmies < 0)
			throw new IllegalArgumentException("Must give a non-negative number of armies");
		
		int freeArmies = this.freeArmies + armies;
		
		if(freeArmies < 0)
			throw new IllegalArgumentException("Way too many armies");
		
		this.freeArmies = freeArmies;
		
	}
	
	public void takeFreeArmies(int armies) {
		
		if(armies < 0)
			throw new IllegalArgumentException("Must take a non-negative number of armies");
		
		if(freeArmies < armies)
			throw new IllegalArgumentException("Can't take more armies than player has");
		
		this.freeArmies -= armies;
		
	}
	
	public void setFreeArmies(int freeArmies) {
		
		if(freeArmies < 0)
			throw new IllegalArgumentException();
		
		this.freeArmies = freeArmies;
		
	}
	
	public int getFreeArmies() {
		return freeArmies;
	}
	
	public boolean hasFreeArmies() {
		return freeArmies != 0;
	}
	
}
