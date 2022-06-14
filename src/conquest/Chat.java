package conquest;

public final class Chat extends Message {
	
	private final Integer playerID;
	private final String chat;
	
	public Chat(Integer playerID, String chat) {
		
		if(playerID == null)
			throw new NullPointerException();
		
		if(chat.isEmpty())
			throw new IllegalArgumentException();
		
		if(chat.length() > 254)
			throw new IllegalArgumentException();
		
		this.playerID = playerID;
		this.chat = chat;
		
	}
	
	public Integer getPlayerID() {
		return playerID;
	}
	
	public String getChat() {
		return chat;
	}
	
	public static Chat fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.CHAT, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		if(length < 1)
			return null;
		
		length -= 1;
		
		int i = HEADER_LENGTH;
		
		char[] str = new char[length];
		for(int j = 0; j < str.length; ++j)
			str[j] = (char) (message[i++] & 0xff);
		
		int playerID = message[i++] & 0xff;
		
		return new Chat(playerID, new String(str));
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.CHAT, chat.length() + 1);
		
		int i = HEADER_LENGTH;
		
		for(int j = 0; j < chat.length(); ++j)
			message[i++] = (byte) chat.charAt(j);
		
		message[i++] = (byte)(int) playerID;
		
		return message;
		
	}
	
	@Override
	public String toString() {
		return chat;
	}

}
