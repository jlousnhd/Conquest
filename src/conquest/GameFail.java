package conquest;

public final class GameFail extends Message {
	
	private final String reason;
	
	public GameFail(String reason) {
		
		this.reason = reason == null ? "" : reason;
		
		if(this.reason.length() > 255)
			throw new IllegalArgumentException();
		
	}
	
	public String getReason() {
		return reason;
	}
	
	public static GameFail fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.GAME_FAIL, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		if(length > 255)
			return null;
		
		int i = HEADER_LENGTH;
		
		char[] str = new char[length];
		for(int j = 0; j < str.length; ++j)
			str[j] = (char) (message[i++] & 0xff);
		
		return new GameFail(new String(str));
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.GAME_FAIL, reason.length());
		
		int i = HEADER_LENGTH;
		
		for(int j = 0; j < reason.length(); ++j)
			message[i++] = (byte) reason.charAt(j);
		
		return message;
		
	}
	
	@Override
	public String toString() {
		return reason;
	}

}
