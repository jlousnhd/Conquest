package conquest;

public final class EndTurn extends Message {

	private static final int LENGTH = 4;
	
	private final Territory from, to;
	private final int armies;
	
	public EndTurn(Territory from, Territory to, int armies) {
		
		if(armies < 0)
			throw new IllegalArgumentException();
		
		if(armies == 0) {
			
			from = null;
			to = null;
			
		} else if(!from.isAdjacentTo(to))
			throw new IllegalArgumentException();
		
		this.from = from;
		this.to = to;
		this.armies = armies;
		
	}
	
	public Territory getFrom() {
		return from;
	}
	
	public Territory getTo() {
		return to;
	}
	
	public int getArmies() {
		return armies;
	}
	
	public boolean isValid(GameData data) {
		
		if(armies != 0) {
			
			if(data.territoryArmies(from) <= armies)
				return false;
			
			PlayerData fromOwner = data.territoryOwner(from);
			if(fromOwner != data.getPlayerTurn())
				return false;
			
			PlayerData toOwner = data.territoryOwner(to);
			if(toOwner != fromOwner)
				return false;
			
		}
		
		if(data.phase() != GamePhase.ATTACKING)
			return false;
		
		return true;
		
	}
	
	public static EndTurn fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.END_TURN, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		if(length != LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		
		int fromID = message[i++] & 0xff;
		int toID = message[i++] & 0xff;
		
		int armies = (message[i++] & 0xff) << 8;
		armies    |= (message[i++] & 0xff);
		
		Territory from = null;
		Territory to = null;
		
		if(armies > 0) {
			
			if(fromID >= Territory.TERRITORY_COUNT)
				return null;
			
			if(toID >= Territory.TERRITORY_COUNT)
				return null;
			
			from = Territory.fromID(fromID);
			to = Territory.fromID(toID);
				
			if(from.isAdjacentTo(to))
				return null;
			
		}
		
		return new EndTurn(from, to, armies);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.END_TURN, LENGTH);
		
		int i = HEADER_LENGTH;
		
		message[i++] = (byte) from.ID;
		message[i++] = (byte) to.ID;
		
		message[i++] = (byte) (armies >>> 8);
		message[i++] = (byte) (armies      );
		
		return message;
		
	}

}
