package conquest;

import java.util.*;

public class Players extends Message {
	
	private static final int LENGTH_MULTIPLIER = 17;
	private static final int MAX_PLAYERS = 15;
	
	public final Map<Integer, String> PLAYERS;
	
	public Players(Map<Integer, String> players) {
		
		PLAYERS = Collections.unmodifiableMap(new TreeMap<Integer, String>(players));
		
		if(PLAYERS.size() > MAX_PLAYERS)
			throw new IllegalArgumentException();
		
	}
	
	public static Players fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.PLAYERS, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		
		int playerCount = length / LENGTH_MULTIPLIER;
		if(length % LENGTH_MULTIPLIER != 0)
			return null;
		
		if(playerCount > MAX_PLAYERS)
			return null;
		
		HashMap<Integer, String> players = new HashMap<Integer, String>();
		int i = HEADER_LENGTH;
		char[] str = new char[MAX_NAME_LENGTH];
		
		for(int j = 0; j < playerCount; ++j) {
			
			Integer id = message[i++] & 0xff;
			
			if(players.containsKey(id))
				return null;
			
			int strlen = MAX_NAME_LENGTH;
			for(int k = 0; k < MAX_NAME_LENGTH; ++k) {
				
				str[k] = (char) (message[i++] & 0xff);
				if(str[k] == 0)
					strlen = Math.min(k, strlen);
				
			}
			
			String name = new String(str, 0, strlen);
			players.put(id, name);
			
		}
		
		return new Players(players);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.PLAYERS, LENGTH_MULTIPLIER * PLAYERS.size());
		
		int i = HEADER_LENGTH;
		
		for(Map.Entry<Integer, String> player : PLAYERS.entrySet()) {
			
			message[i++] = (byte)(int) player.getKey();
			
			String name = player.getValue();
			
			for(int j = 0; j < 16; ++j)
				message[i++] = (j >= name.length()) ? 0 : (byte) name.charAt(j);
			
		}
		
		return message;
		
	}

}
