package conquest;

public final class Hello extends Message {
	
	private static final int LENGTH = 26;
	
	public static final int MAX_ID = 254;
	public static final int VERSION = 1;
	
	public final String NAME;
	public final int ID;
	
	public Hello(String name, int id) {
		
		if(id < 0 || id > MAX_ID)
			throw new IllegalArgumentException();
		
		if(name.length() > MAX_NAME_LENGTH)
			throw new IllegalArgumentException();
		
		this.NAME = name;
		this.ID = id;
		
	}
	
	public static Hello fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.HELLO, message, length))
			return null;
		
		if(length != LENGTH + HEADER_LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		char[] str = new char[MAX_NAME_LENGTH];
		
		// "CONQUEST"
		for(int j = 0; j < 8; ++j)
			str[j] = (char) (message[i++] & 0xff);
		
		if(!new String(str, 0, 8).equals("CONQUEST"))
			return null;
		
		// Version
		if(message[i++] != VERSION)
			return null;
		
		// ID
		int id = message[i++] & 0xff;
		
		// Name
		int strlen = 16;
		for(int j = 0; j < MAX_NAME_LENGTH; ++j) {
			
			str[j] = (char) (message[i++] & 0xff);
			
			if(str[j] == 0)
				strlen = Math.min(j, strlen);
			
		}
		
		String name;
		if((name = new String(str, 0, strlen)).isEmpty())
			return null;
		
		return new Hello(name, id);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.HELLO, LENGTH);
		
		int i = HEADER_LENGTH;
		
		message[i++] = 'C';
		message[i++] = 'O';
		message[i++] = 'N';
		message[i++] = 'Q';
		message[i++] = 'U';
		message[i++] = 'E';
		message[i++] = 'S';
		message[i++] = 'T';
		
		message[i++] = VERSION;
		message[i++] = (byte) ID;
		
		for(int j = 0; j < NAME.length(); ++j)
			message[i++] = (byte) NAME.charAt(j);
		
		return message;
		
		
	}

}
