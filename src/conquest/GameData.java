package conquest;

import java.util.*;

public final class GameData extends Message {
	
	private static final int STATIC_LENGTH = 133;
	private static final int PER_PLAYER_LENGTH = 3;
	
	private static final int[] EXCHANGE_CHART = {
		4, 6, 8, 10, 12, 15
	};
	
	private final TreeMap<Integer, PlayerData> players;
	private PlayerData playerTurn;
	private final int[] territoryArmies;
	private int exchanges;
	private final ArrayList<Card> deck;
	private boolean conqueredThisTurn;
	
	private Territory lastConquered, lastConquering;
	
	// Initializes a new game with the specified players, a random starting
	// player with the appropriate number of free armies
	public GameData(Set<Integer> playerIDs) {
		
		int playerCount = playerIDs.size();
		
		if(playerCount < 2 || playerCount > 6)
			throw new IllegalArgumentException("Must be 2-6 players");
		
		players = new TreeMap<Integer, PlayerData>();
		
		for(Integer id : playerIDs)
			players.put(id, new PlayerData(id));
		
		assert(players.size() == playerCount);
		
		territoryArmies = new int[Territory.TERRITORY_COUNT];
		
		deck = new ArrayList<Card>(Card.VALUES);
		Collections.shuffle(deck);
		
	}
	
	public void initializeGame() {
		
		if(playerTurn != null)
			throw new IllegalStateException();
		
		int startingArmies = 40 - ((players.size() - 2) * 5);
		int firstPlayer = Util.RANDOM.nextInt(players.size());
		
		for(PlayerData player : players.values()) {
			
			player.giveFreeArmies(startingArmies);
			
			if(firstPlayer == 0)
				this.playerTurn = player;
			
			--firstPlayer;
			
		}
		
	}
	
	public Territory getLastConquered() {
		return lastConquered;
	}
	
	public Territory getLastConquering() {
		return lastConquering;
	}
	
	public int territoryArmies(Territory territory) {
		return territoryArmies[territory.ID];
	}
	
	public PlayerData getPlayerTurn() {
		return playerTurn;
	}
	
	public boolean playerOwnsTerritory(Integer playerID, Territory territory) {
		
		PlayerData owner = territoryOwner(territory);
		
		return owner != null && owner.getID().equals(playerID);
		
	}
	
	public PlayerData territoryOwner(Territory territory) {
		
		for(PlayerData player : players.values())
			if(player.holdsTerritory(territory))
				return player;
		
		return null;
		
	}
	
	public PlayerData[] territoryOwners() {
		
		PlayerData[] owners = new PlayerData[Territory.TERRITORY_COUNT];
		
		for(PlayerData player : players.values())
			for(int i = 0; i < Territory.TERRITORY_COUNT; ++i)
				if(player.holdsTerritory(Territory.fromID(i)))
					owners[i] = player;
		
		return owners;
		
	}
	
	public void updateOwnersArmies(Collection<String> events, Territory territory, int ownerID, int armies) {
		
		PlayerData oldOwner = territoryOwner(territory);
		PlayerData owner = players.get(ownerID);
		
		int oldArmies = territoryArmies[territory.ID];
		
		// Territory did not change hands
		if(owner == oldOwner) {
			
			if(armies < oldArmies)
				events.add(owner + " lost " + (oldArmies - armies) + " in " + territory + ".");
			
			else if(armies > oldArmies)
				events.add(owner + " gained " + (armies - oldArmies) + " in " + territory + ".");
			
		}
		
		// Territory changed hands
		else {
			
			// No one owned it before
			if(oldOwner == null) {
				
				events.add(owner + " claimed " + territory + " and gained " + armies + " armies there.");
				owner.giveTerritory(territory);
				
			}
			
			// Someone lost it
			else {
				
				events.add(owner + " took " + territory + " from " + oldOwner + " and gained " + armies + "armies there.");
				owner.giveTerritory(territory);
				oldOwner.takeTerritory(territory);
				
			}
			
		}
		
		this.territoryArmies[territory.ID] = armies;
		
	}
	
	public void updatePlayerTurn(Collection<String> events, int playerTurnID) {
		
		PlayerData playerTurn = players.get(playerTurnID);
		
		if(playerTurn == null)
			throw new IllegalArgumentException();
		
		if(playerTurn == this.playerTurn)
			return;
		
		events.add(this.playerTurn + "'s turn ended and " + playerTurn + "'s turn begins.");
		
		this.playerTurn = playerTurn;
		
	}
	
	public PlayerData winner() {
		
		for(PlayerData player : players.values()) {
			
			long mask = player.getOwnedTerritoriesAsMask();
			if((mask & Territory.ALL_TERRITORIES_MASK) == Territory.ALL_TERRITORIES_MASK)
				return player;
			
		}
		
		return null;
		
	}
	
	public GamePhase phase() {
		
		long ownedTerritories = 0L;
		
		for(PlayerData player : players.values()) {
			
			long mask = player.getOwnedTerritoriesAsMask();
			if((mask & Territory.ALL_TERRITORIES_MASK) == Territory.ALL_TERRITORIES_MASK)
				return GamePhase.VICTORY;
			
			ownedTerritories |= mask;
			
		}
		
		if((ownedTerritories & Territory.ALL_TERRITORIES_MASK) != Territory.ALL_TERRITORIES_MASK)
			return GamePhase.CLAIMING;
		
		int playersWithFreeArmies = 0;
		
		for(PlayerData player : players.values())
			playersWithFreeArmies += player.hasFreeArmies() ? 1 : 0;
		
		if(playersWithFreeArmies > 1)
			return GamePhase.CLAIMING;
		
		if(playerTurn.hasFreeArmies())
			return GamePhase.PLACEMENT;
		
		return GamePhase.ATTACKING;
		
	}
	
	public static GameData fromBytes(byte[] message, int length, Set<Integer> ids, Integer myID) {
		
		if(!validateHeader(MessageType.GAME_STATE, message, length))
			return null;
		
		length -= (HEADER_LENGTH + STATIC_LENGTH);
		int playerCount = length / PER_PLAYER_LENGTH;
		if(length % PER_PLAYER_LENGTH != 0)
			return null;
		
		if(playerCount < 2 || playerCount > 6)
			return null;
		
		int i = HEADER_LENGTH;
		
		int playerTurn = message[i++];
		
		GameData data = new GameData(ids);
		data.playerTurn = data.players.get(playerTurn);
		
		// Territory owners
		for(int j = 0; j < Territory.TERRITORY_COUNT; ++j) {
			
			PlayerData player = data.players.get(message[i++] & 0xff);
			
			if(player != null)
				player.giveTerritory(Territory.fromID(j));
			
		}
		
		// Territory armies
		for(int j = 0; j < data.territoryArmies.length; ++j) {
			
			int armies = (message[i++] & 0xff) << 8;
			armies    |= (message[i++] & 0xff);
			
			data.territoryArmies[j] = armies;
			
		}
		
		// Player stats
		for(PlayerData player : data.players.values()) {
			
			int freeArmies = (message[i++] & 0xff) << 8;
			freeArmies    |= (message[i++] & 0xff);
			player.setFreeArmies(freeArmies);
			
			int cardsHeld = message[i++];
			player.setCardCount(cardsHeld);
			
		}
		
		// Current player cards
		long myCards = (message[i++] & 0xffL) << 40;
		myCards     |= (message[i++] & 0xffL) << 32;
		myCards     |= (message[i++] & 0xffL) << 24;
		myCards     |= (message[i++] & 0xffL) << 16;
		myCards     |= (message[i++] & 0xffL) << 8;
		myCards     |= (message[i++] & 0xffL);
		
		data.players.get(myID).setOwnedCards(myCards & Card.ALL_CARDS_MASK);
		
		return data;
		
	}

	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.GAME_STATE, STATIC_LENGTH + PER_PLAYER_LENGTH * players.size());
		
		int i = HEADER_LENGTH;
		
		// Player turn
		message[i++] = (byte)(int) playerTurn.getID();
		
		// Territory owners
		PlayerData[] territoryOwners = territoryOwners();
		
		for(int j = 0; j < territoryOwners.length; ++j)
			message[i++] = territoryOwners[j] == null ? -1 : (byte)(int) territoryOwners[j].getID();
		
		// Territory armies
		for(int j = 0; j < territoryArmies.length; ++j) {
			
			int armies = territoryArmies[j];
			
			message[i++] = (byte) (armies >>> 8);
			message[i++] = (byte) (armies      );
			
		}
		
		// Players
		for(PlayerData player : players.values()) {
			
			int freeArmies = player.getFreeArmies();
			
			message[i++] = (byte) (freeArmies >>> 8);
			message[i++] = (byte) (freeArmies      );
			
			int cardsHeld = player.cardCount();
			message[i++] = (byte) cardsHeld;
			
		}
		
		// Current player cards
		
		long cards = players.get(currentPlayer).getOwnedCardsAsMask();
		
		message[i++] = (byte) (cards >>> 40);
		message[i++] = (byte) (cards >>> 32);
		message[i++] = (byte) (cards >>> 24);
		message[i++] = (byte) (cards >>> 16);
		message[i++] = (byte) (cards >>>  8);
		message[i++] = (byte) (cards       );
		
		return message;
		
	}
	
	public boolean areAllTerritoriesOwned() {
		
		long ownedTerritories = 0L;
		
		for(PlayerData player : players.values())
			ownedTerritories |= player.getOwnedTerritoriesAsMask();
		
		return ownedTerritories == Territory.ALL_TERRITORIES_MASK;
		
	}
	
	public void nextTurn() {
		
		GamePhase phase = phase();
		
		if(phase == GamePhase.VICTORY)
			return;
		
		// Give old player a card
		if(conqueredThisTurn && !deck.isEmpty()) {
			
			Card card = deck.remove(deck.size() - 1);
			playerTurn.giveCard(card);
			
		}
		
		conqueredThisTurn = false;
		
		// Next player's turn, skip over players who are out
		do {
			
			Map.Entry<Integer, PlayerData> next = players.higherEntry(playerTurn.getID());
			playerTurn = next != null ? next.getValue() : players.firstEntry().getValue();
			
			if(phase == GamePhase.CLAIMING)
				break;
			
		} while(playerTurn.territoryCount() < 1);
		
		// Give armies
		if(!playerTurn.hasFreeArmies())
			playerTurn.giveFreeArmies(playerTurn.calculateIncome());
		
		lastConquered = lastConquering = null;
		
	}
	
	public boolean doMove(Integer requestingPlayerID, Attack move) {
		
		PlayerData requestingPlayer = players.get(requestingPlayerID);
		
		if(requestingPlayer == null || playerTurn != requestingPlayer)
			return false;
		
		if(!move.isValid(this))
			return false;
		
		// Roll dice
		int defendingDice = Math.min(2, territoryArmies(move.getTo()));
		defendingDice = Math.min(defendingDice, move.getDice());
		
		int[] diceAttacker = new int[move.getDice()];
		int[] diceDefender = new int[defendingDice];
		
		for(int i = 0; i < diceAttacker.length; ++i)
			diceAttacker[i] = Util.RANDOM.nextInt(6);
		
		for(int i = 0; i < diceDefender.length; ++i)
			diceDefender[i] = Util.RANDOM.nextInt(6);
		
		// Compare rolls
		Util.sortDescending(diceAttacker);
		Util.sortDescending(diceDefender);
		
		int attackerLoss = 0;
		int defenderLoss = 0;
		
		for(int i = 0; i < diceDefender.length; ++i) {
			
			if(diceAttacker[i] > diceDefender[i])
				++defenderLoss;
			
			else
				++attackerLoss;
			
		}
		
		// Affect armies
		territoryArmies[move.getFrom().ID] -= attackerLoss;
		territoryArmies[move.getTo()  .ID] -= defenderLoss;
		
		// Was territory conquered?
		if(territoryArmies[move.getTo().ID] == 0) {
			
			conqueredThisTurn = true;
			
			lastConquering = move.getFrom();
			lastConquered = move.getTo();
			
			PlayerData conquered = territoryOwner(lastConquered);
			PlayerData conquering = territoryOwner(lastConquering);
			
			conquering.giveTerritory(lastConquered);
			conquered.takeTerritory(lastConquered);
			
			--territoryArmies[lastConquering.ID];
			++territoryArmies[lastConquered.ID];
			
		}
		
		else
			lastConquered = lastConquering = null;
		
		return true;
		
	}
	
	public boolean doMove(Integer requestingPlayerID, EndTurn move) {
		
		PlayerData requestingPlayer = players.get(requestingPlayerID);
		
		if(requestingPlayer == null || playerTurn != requestingPlayer)
			return false;
		
		if(!move.isValid(this))
			return false;
		
		// Move armies
		territoryArmies[move.getFrom().ID] -= move.getArmies();
		territoryArmies[move.getTo()  .ID] += move.getArmies();
		
		// Next player's turn
		nextTurn();
		
		return true;
		
	}
	
	public boolean doMove(Integer requestingPlayerID, ExchangeCards move) {
		
		PlayerData requestingPlayer = players.get(requestingPlayerID);
		
		if(requestingPlayer == null || playerTurn != requestingPlayer)
			return false;
		
		if(!move.isValid(this))
			return false;
		
		// Exchange cards
		EnumSet<Card> cards = Card.maskToSet(move.cardsAsMask());
		
		playerTurn.takeCards(cards);
		deck.addAll(cards);
		Collections.shuffle(deck);
		
		playerTurn.giveFreeArmies(exchange());
		
		return true;
		
	}
	
	public boolean doMove(Integer requestingPlayerID, PlaceArmies move) {
		
		PlayerData requestingPlayer = players.get(requestingPlayerID);
		
		if(requestingPlayer == null || playerTurn != requestingPlayer)
			return false;
		
		if(!move.isValid(this))
			return false;
		
		GamePhase phase = phase();
		
		// Place armies
		playerTurn.takeFreeArmies(move.getArmies());
		
		int oldArmies = territoryArmies[move.getTerritory().ID];
		territoryArmies[move.getTerritory().ID] = oldArmies + move.getArmies();
		
		// If we're claiming, give player territory and go to next turn
		if(oldArmies == 0)
			playerTurn.giveTerritory(move.getTerritory());
		
		if(phase == GamePhase.CLAIMING)
			nextTurn();
		
		lastConquered = lastConquering = null;
		
		return true;
		
	}
	
	public boolean doMove(Integer requestingPlayerID, TransferArmies move) {
		
		PlayerData requestingPlayer = players.get(requestingPlayerID);
		
		if(requestingPlayer == null || playerTurn != requestingPlayer)
			return false;
		
		if(!move.isValid(this, lastConquering, lastConquered))
			return false;
		
		// Transfer armies
		territoryArmies[move.getFrom().ID] -= move.getArmies();
		territoryArmies[move.getTo()  .ID] += move.getArmies();
		
		lastConquered = lastConquering = null;
		
		return true;
		
	}
	
	private int exchange() {
		
		if(exchanges <= EXCHANGE_CHART.length)
			return EXCHANGE_CHART[exchanges++];
		
		return EXCHANGE_CHART[EXCHANGE_CHART.length - 1] + 5 * (exchanges - EXCHANGE_CHART.length + 1);
		
	}
	
}
