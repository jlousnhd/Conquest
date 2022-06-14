package conquest;

public final class PlaceArmies extends Message {
	
	private static final int LENGTH = 3;
	
	private final Territory territory;
	private final int armies;
	
	public PlaceArmies(Territory territory, int armies) {
		
		if(territory == null)
			throw new NullPointerException();
		
		if(armies < 1)
			throw new IllegalArgumentException();
		
		this.territory = territory;
		this.armies = armies;
		
	}
	
	public int getArmies() {
		return armies;
	}
	
	public Territory getTerritory() {
		return territory;
	}
	
	public boolean isValid(GameData data) {
		
		PlayerData playerTurn = data.getPlayerTurn();
		
		if(playerTurn.getFreeArmies() < armies)
			return false;
		
		GamePhase phase = data.phase();
		
		PlayerData territoryOwner = data.territoryOwner(territory);
		
		// Placing armies during player's turn (can place as many as desired on owned territory)
		if(phase == GamePhase.PLACEMENT)
			return playerTurn == territoryOwner;
		
		// Claiming unowned territory
		return phase == GamePhase.CLAIMING && armies == 1 && (territoryOwner == null || (territoryOwner == playerTurn && data.areAllTerritoriesOwned()));
		
	}
	
	public static PlaceArmies fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.PLACE_ARMY, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		
		if(length != LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		
		int territoryID = message[i++] & 0xff;
		
		if(territoryID >= Territory.TERRITORY_COUNT)
			return null;
		
		int armies = (message[i++] & 0xff) << 8;
		armies    |= (message[i++] & 0xff);
		
		return new PlaceArmies(Territory.fromID(territoryID), armies);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.PLACE_ARMY, LENGTH);
		
		int i = HEADER_LENGTH;
		
		message[i++] = (byte) territory.ID;
		
		message[i++] = (byte) (armies >>> 8);
		message[i++] = (byte) (armies      );
		
		return message;
		
	}

}
