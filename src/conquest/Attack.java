package conquest;

public final class Attack extends Message {
	
	private static final int LENGTH = 3;
	
	private final Territory from, to;
	private final int dice;
	
	public Attack(Territory from, Territory to, int dice) {
		
		if(dice < 1 || dice > 3)
			throw new IllegalArgumentException();
		
		if(!from.isAdjacentTo(to))
			throw new IllegalArgumentException("Territories must be adjacent");
		
		this.from = from;
		this.to = to;
		this.dice = dice;
		
	}
	
	public Territory getFrom() {
		return from;
	}
	
	public Territory getTo() {
		return to;
	}
	
	public int getDice() {
		return dice;
	}
	
	public boolean isValid(GameData data) {
		
		int fromArmies = data.territoryArmies(from);
		
		if(fromArmies <= dice)
			return false;
		
		PlayerData fromOwner = data.territoryOwner(from);
		if(data.getPlayerTurn() != fromOwner)
			return false;
		
		PlayerData toOwner = data.territoryOwner(to);
		if(fromOwner == toOwner)
			return false;
		
		if(data.phase() != GamePhase.ATTACKING)
			return false;
		
		return true;
		
	}
	
	public static Attack fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.ATTACK, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		if(length != LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		
		int fromID = message[i++] & 0xff;
		int toID   = message[i++] & 0xff;
		int dice   = message[i++] & 0xff;
		
		if(dice < 1 || dice > 3)
			return null;
		
		if(fromID >= Territory.TERRITORY_COUNT || toID >= Territory.TERRITORY_COUNT)
			return null;
		
		Territory from = Territory.fromID(fromID);
		Territory to = Territory.fromID(toID);
		
		if(!from.isAdjacentTo(to))
			return null;
		
		return new Attack(from, to, dice);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.ATTACK, LENGTH);
		
		int i = HEADER_LENGTH;
		
		message[i++] = (byte) from.ID;
		message[i++] = (byte) to.ID;
		message[i++] = (byte) dice;
		
		return message;
		
	}

}
