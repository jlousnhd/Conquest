package conquest;

public abstract class Message {
	
	protected static final int HEADER_LENGTH = 2;
	
	public static final int MAX_NAME_LENGTH = 16;
	
	protected static byte[] createMessage(int type, int dataLength) {
		
		assert(type >= 0 && type <= 255);
		assert(dataLength >= 0 && dataLength <= 255);
		
		byte[] buffer = new byte[HEADER_LENGTH + dataLength];
		
		buffer[0] = (byte) type;
		buffer[1] = (byte) dataLength;
		
		return buffer;
		
	}
	
	protected static boolean validateHeader(int type, byte[] message, int length) {
		
		if((message[0] & 0xff) != type)
			return false;
		
		if((message[1] & 0xff) != length - HEADER_LENGTH)
			return false;
		
		return true;
		
	}
	
	public abstract byte[] toBytes(Integer currentPlayer);
	
}
