package conquest;

import java.util.*;

public class ExchangeCards extends Message {
	
	private static final int LENGTH = 3;
	
	private final EnumSet<Card> cards;
	
	public ExchangeCards(EnumSet<Card> cards) {
		
		if(cards.size() != 3)
			throw new IllegalArgumentException();
		
		this.cards = cards.clone();
		
	}
	
	public boolean isValid(GameData data) {
		
		if(!data.getPlayerTurn().holdsCards(cards))
			return false;
		
		return Card.containsExchangeable(cards);
		
	}
	
	public long cardsAsMask() {
		return Card.setToMask(cards);
	}
	
	public static ExchangeCards fromBytes(byte[] message, int length) {
		
		if(!validateHeader(MessageType.EXCHANGE_CARDS, message, length))
			return null;
		
		length -= HEADER_LENGTH;
		
		if(length != LENGTH)
			return null;
		
		int i = HEADER_LENGTH;
		
		EnumSet<Card> cards = EnumSet.noneOf(Card.class);
		
		for(int j = 0; j < 3; ++j) {
			
			int id = message[i++] & 0xff;
			
			if(id >= Card.CARD_COUNT)
				return null;
			
			cards.add(Card.VALUES.get(id));
			
		}
		
		if(cards.size() != 3)
			return null;
		
		return new ExchangeCards(cards);
		
	}
	
	@Override
	public byte[] toBytes(Integer currentPlayer) {
		
		byte[] message = createMessage(MessageType.EXCHANGE_CARDS, LENGTH);
		
		int i = HEADER_LENGTH;
		
		for(Card card : cards)
			message[i++] = (byte) card.ID;
		
		return message;
		
	}

}
