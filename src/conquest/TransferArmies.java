package conquest;

public class TransferArmies extends Message {

	private static final int LENGTH = 4;
	
	private final Territory from, to;
	private final int armies;
	
	public TransferArmies(Territory from, Territory to, int armies) {
		
		if(from.isAdjacentTo(to))
			throw new IllegalArgumentException();
		
		if(armies < 1)
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
	
	public boolean isValid(GameData data, Territory lastConquering, Territory lastConquered) {
		
		if(lastConquering != from)
			return false;
		
		if(lastConquered != to)
			return false;
		
		if(data.territoryArmies(from) <= armies)
			return false;
		
		PlayerData fromOwner = data.territoryOwner(from);
		
		if(data.getPlayerTurn() != fromOwner)
			return false;
		
		if(data.territoryOwner(to) != fromOwner)
			return false;
		
		if(data.phase() != GamePhase.ATTACKING)
			return false;
		
		return true;
		
	}
	
	public static TransferArmies fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.TRANSFER_ARMIES, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		if(length != LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		
		int fromID = message[i++] & 0xff;
		
		if(fromID >= Territory.TERRITORY_COUNT)
			return null;
		
		int toID = message[i++] & 0xff;
		
		if(toID >= Territory.TERRITORY_COUNT)
			return null;
		
		int armies = (message[i++] & 0xff) << 8;
		armies    |= (message[i++] & 0xff);
		
		if(armies < 1)
			return null;
		
		Territory from = Territory.fromID(fromID);
		Territory to = Territory.fromID(toID);
		
		if(from.isAdjacentTo(to))
			return null;
		
		return new TransferArmies(from, to, armies);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.TRANSFER_ARMIES, LENGTH);
		
		int i = HEADER_LENGTH;
		
		message[i++] = (byte) from.ID;
		message[i++] = (byte) to.ID;
		
		message[i++] = (byte) (armies >>> 8);
		message[i++] = (byte) (armies      );
		
		return message;
		
	}

}
