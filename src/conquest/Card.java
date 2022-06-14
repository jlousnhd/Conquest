package conquest;
import java.util.*;

public enum Card {
	
	ALASKA              (CardType.INFANTRY,  Territory.ALASKA),
	WESTERN_CANADA      (CardType.CAVALRY,   Territory.WESTERN_CANADA),
	CENTRAL_AMERICA     (CardType.ARTILLERY, Territory.CENTRAL_AMERICA),
	EASTERN_US          (CardType.ARTILLERY, Territory.EASTERN_US),
	GREENLAND           (CardType.CAVALRY,   Territory.GREENLAND),
	NORTHWEST_TERRITORY (CardType.ARTILLERY, Territory.NORTHWEST_TERRITORY),
	CENTRAL_CANADA      (CardType.CAVALRY,   Territory.CENTRAL_CANADA),
	EASTERN_CANADA      (CardType.CAVALRY,   Territory.EASTERN_CANADA),
	WESTERN_US          (CardType.ARTILLERY, Territory.WESTERN_US),
	
	ARGENTINA           (CardType.INFANTRY,  Territory.ARGENTINA),
	BRAZIL              (CardType.ARTILLERY, Territory.BRAZIL),
	PERU                (CardType.INFANTRY,  Territory.PERU),
	VENEZUELA           (CardType.INFANTRY,  Territory.VENEZUELA),
	
	CENTRAL_AFRICA      (CardType.INFANTRY,  Territory.CENTRAL_AFRICA),
	EAST_AFRICA         (CardType.INFANTRY,  Territory.EAST_AFRICA),
	EGYPT               (CardType.INFANTRY,  Territory.EGYPT),
	MADAGASCAR          (CardType.CAVALRY,   Territory.MADAGASCAR),
	NORTH_AFRICA        (CardType.CAVALRY,   Territory.NORTH_AFRICA),
	SOUTH_AFRICA        (CardType.ARTILLERY, Territory.SOUTH_AFRICA),
	
	GREAT_BRITAIN       (CardType.ARTILLERY, Territory.GREAT_BRITAIN),
	ICELAND             (CardType.INFANTRY,  Territory.ICELAND),
	NORTHERN_EUROPE     (CardType.ARTILLERY, Territory.NORTHERN_EUROPE),
	SCANDINAVIA         (CardType.CAVALRY,   Territory.SCANDINAVIA),
	SOUTHERN_EUROPE     (CardType.ARTILLERY, Territory.SOUTHERN_EUROPE),
	EASTERN_EUROPE      (CardType.CAVALRY,   Territory.EASTERN_EUROPE),
	WESTERN_EUROPE      (CardType.ARTILLERY, Territory.WESTERN_EUROPE),
	
	AFGHANISTAN         (CardType.CAVALRY,   Territory.AFGHANISTAN),
	CHINA               (CardType.INFANTRY,  Territory.CHINA),
	INDIA               (CardType.CAVALRY,   Territory.INDIA),
	IRKUTSK             (CardType.CAVALRY,   Territory.IRKUTSK),
	JAPAN               (CardType.ARTILLERY, Territory.JAPAN),
	KAMCHATKA           (CardType.INFANTRY,  Territory.KAMCHATKA),
	MIDDLE_EAST         (CardType.INFANTRY,  Territory.MIDDLE_EAST),
	MONGOLIA            (CardType.INFANTRY,  Territory.MONGOLIA),
	SOUTHEAST_ASIA      (CardType.INFANTRY,  Territory.SOUTHEAST_ASIA),
	SIBERIA             (CardType.CAVALRY,   Territory.SIBERIA),
	URAL                (CardType.CAVALRY,   Territory.URAL),
	YAKUTSK             (CardType.CAVALRY,   Territory.YAKUTSK),
	
	EASTERN_AUSTRALIA   (CardType.ARTILLERY, Territory.EASTERN_AUSTRALIA),
	INDONESIA           (CardType.ARTILLERY, Territory.INDONESIA),
	NEW_GUINEA          (CardType.INFANTRY,  Territory.NEW_GUINEA),
	WESTERN_AUSTRALIA   (CardType.ARTILLERY, Territory.WESTERN_AUSTRALIA),
	
	WILD_1              (CardType.WILD,      null),
	WILD_2              (CardType.WILD,      null);
	
	public static final List<Card> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	public static final int CARD_COUNT = VALUES.size();
	public static final long ALL_CARDS_MASK = ~(0xffffffffffffffffL << Territory.TERRITORY_COUNT);
	
	public final CardType TYPE;
	public final Territory TERRITORY;
	public final long MASK;
	public final int ID;
	
	public final int INFANTRY_COUNT;
	public final int CAVALRY_COUNT;
	public final int ARTILLERY_COUNT;
	public final int WILD_COUNT;
	
	Card(CardType type, Territory territory) {
		
		this.TYPE = type;
		this.TERRITORY = territory;
		
		this.INFANTRY_COUNT  = type == CardType.INFANTRY  ? 1 : 0;
		this.CAVALRY_COUNT   = type == CardType.CAVALRY   ? 1 : 0;
		this.ARTILLERY_COUNT = type == CardType.ARTILLERY ? 1 : 0;
		this.WILD_COUNT      = type == CardType.WILD      ? 1 : 0;
		
		this.MASK = 1L << ordinal();
		this.ID = ordinal();
		
	}
	
	public static EnumSet<Card> maskToSet(long mask) {
		
		mask &= ALL_CARDS_MASK;
		
		EnumSet<Card> set = EnumSet.noneOf(Card.class);
		
		// Go through each set bit, single it out and convert it to an index
		while(mask != 0L) {
			
			long bitRemoved = mask & (mask - 1L);
			long bitAlone = mask ^ bitRemoved;
			
			mask = bitRemoved;
			
			set.add(VALUES.get(Util.log2OfPowerOf2(bitAlone)));
			
		}
		
		return set;
		
	}
	
	public static long setToMask(EnumSet<Card> set) {
		
		long mask = 0L;
		
		for(Card card : set)
			mask |= card.MASK;
		
		return mask;
		
	}
	
	private static boolean containsExchangeable(int infantry, int cavalry, int artillery, int wild) {
		
		// If there are one or fewer zeros, we can match a run of cards
		int zeros = 0;
		
		zeros += infantry  == 0 ? 1 : 0;
		zeros += cavalry   == 0 ? 1 : 0;
		zeros += artillery == 0 ? 1 : 0;
		zeros += wild      == 0 ? 1 : 0;
		
		if(zeros <= 1)
			return true;
		
		// If there are 3 of any card type + wilds, we can match the same type
		infantry  += wild;
		cavalry   += wild;
		artillery += wild;
		
		if(infantry >= 3)
			return true;
		
		if(cavalry >= 3)
			return true;
		
		if(artillery >= 3)
			return true;
		
		// No matches can be made
		return false;
		
	}
	
	public static boolean containsExchangeable(long mask) {
		
		int infantry = 0;
		int cavalry = 0;
		int artillery = 0;
		int wild = 0;
		
		// Go through each set bit, single it out and convert it to an index
		while(mask != 0L) {
			
			long bitRemoved = mask & (mask - 1L);
			long bitAlone = mask ^ bitRemoved;
			
			mask = bitRemoved;
			
			Card card = VALUES.get(Util.log2OfPowerOf2(bitAlone));
			
			infantry  += card.INFANTRY_COUNT;
			cavalry   += card.CAVALRY_COUNT;
			artillery += card.ARTILLERY_COUNT;
			wild      += card.WILD_COUNT;
			
		}
		
		return containsExchangeable(infantry, cavalry, artillery, wild);
		
	}
	
	public static boolean containsExchangeable(EnumSet<Card> cards) {
		
		int infantry = 0;
		int cavalry = 0;
		int artillery = 0;
		int wild = 0;
		
		for(Card card : cards) {
			
			infantry  += card.INFANTRY_COUNT;
			cavalry   += card.CAVALRY_COUNT;
			artillery += card.ARTILLERY_COUNT;
			wild      += card.WILD_COUNT;
			
		}
		
		return containsExchangeable(infantry, cavalry, artillery, wild);
		
	}
	
}
