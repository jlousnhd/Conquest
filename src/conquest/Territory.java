package conquest;
import java.util.*;

public enum Territory {
	
	// North America
	ALASKA              ("Alaska"),
	WESTERN_CANADA      ("Western Canada"),
	CENTRAL_AMERICA     ("Central America"),
	EASTERN_US          ("Eastern United States"),
	GREENLAND           ("Greenland"),
	NORTHWEST_TERRITORY ("Northwest Territory"),
	CENTRAL_CANADA      ("Central Canada"),
	EASTERN_CANADA      ("Eastern Canada"),
	WESTERN_US          ("Western United States"),
	
	// South America
	ARGENTINA           ("Argentina"),
	BRAZIL              ("Brazil"),
	PERU                ("Peru"),
	VENEZUELA           ("Venezuela"),
	
	// Africa
	CENTRAL_AFRICA      ("Central Africa"),
	EAST_AFRICA         ("East Africa"),
	EGYPT               ("Egypt"),
	MADAGASCAR          ("Madagascar"),
	NORTH_AFRICA        ("North Africa"),
	SOUTH_AFRICA        ("South Africa"),
	
	// Europe
	GREAT_BRITAIN       ("Great Britain"),
	ICELAND             ("Iceland"),
	NORTHERN_EUROPE     ("Northern Europe"),
	SCANDINAVIA         ("Scandinavia"),
	SOUTHERN_EUROPE     ("Southern Europe"),
	EASTERN_EUROPE      ("Eastern Europe"),
	WESTERN_EUROPE      ("Western Europe"),
	
	// Asia
	AFGHANISTAN         ("Afghanistan"),
	CHINA               ("China"),
	INDIA               ("India"),
	IRKUTSK             ("Irkutsk"),
	JAPAN               ("Japan"),
	KAMCHATKA           ("Kamchatka"),
	MIDDLE_EAST         ("Middle East"),
	MONGOLIA            ("Mongolia"),
	SOUTHEAST_ASIA      ("Southeast Asia"),
	SIBERIA             ("Siberia"),
	URAL                ("Ural"),
	YAKUTSK             ("Yakutsk"),
	
	// Australia
	EASTERN_AUSTRALIA   ("Eastern Australia"),
	INDONESIA           ("Indonesia"),
	NEW_GUINEA          ("New Guinea"),
	WESTERN_AUSTRALIA   ("Western Australia");
	
	public final String NAME;
	public final long MASK;
	public final int ID;
	
	Territory(String name) {
		
		this.NAME = name;
		this.MASK = 1L << ordinal();
		this.ID = ordinal();
		
	}
	
	private static final Territory[] VALUES = values();
	public static final int TERRITORY_COUNT = VALUES.length;
	public static final long ALL_TERRITORIES_MASK = ~(0xffffffffffffffffL << Territory.TERRITORY_COUNT);
	private static final long[] ADJACENCIES = initiateAdjacencies();
	
	public boolean isAdjacentTo(Territory territory) {
		return (ADJACENCIES[ID] & territory.MASK) == territory.MASK;
	}
	
	public long getAdjacencies() {
		return ADJACENCIES[ID];
	}
	
	private static long[] initiateAdjacencies() {
		
		long[] adjacencies = new long[VALUES.length];
		
		// North America
		createAdjacency(adjacencies, ALASKA, KAMCHATKA);
		createAdjacency(adjacencies, ALASKA, NORTHWEST_TERRITORY);
		createAdjacency(adjacencies, ALASKA, WESTERN_CANADA);
		createAdjacency(adjacencies, NORTHWEST_TERRITORY, WESTERN_CANADA);
		createAdjacency(adjacencies, NORTHWEST_TERRITORY, CENTRAL_CANADA);
		createAdjacency(adjacencies, NORTHWEST_TERRITORY, GREENLAND);
		createAdjacency(adjacencies, GREENLAND, CENTRAL_CANADA);
		createAdjacency(adjacencies, GREENLAND, EASTERN_CANADA);
		createAdjacency(adjacencies, GREENLAND, ICELAND);
		createAdjacency(adjacencies, WESTERN_CANADA, CENTRAL_CANADA);
		createAdjacency(adjacencies, WESTERN_CANADA, WESTERN_US);
		createAdjacency(adjacencies, CENTRAL_CANADA, WESTERN_US);
		createAdjacency(adjacencies, CENTRAL_CANADA, EASTERN_US);
		createAdjacency(adjacencies, CENTRAL_CANADA, EASTERN_CANADA);
		createAdjacency(adjacencies, EASTERN_CANADA, EASTERN_US);
		createAdjacency(adjacencies, WESTERN_US, EASTERN_US);
		createAdjacency(adjacencies, WESTERN_US, CENTRAL_AMERICA);
		createAdjacency(adjacencies, EASTERN_US, CENTRAL_AMERICA);
		createAdjacency(adjacencies, CENTRAL_AMERICA, VENEZUELA);
		
		// South America
		createAdjacency(adjacencies, VENEZUELA, PERU);
		createAdjacency(adjacencies, VENEZUELA, BRAZIL);
		createAdjacency(adjacencies, PERU, BRAZIL);
		createAdjacency(adjacencies, PERU, ARGENTINA);
		createAdjacency(adjacencies, ARGENTINA, BRAZIL);
		createAdjacency(adjacencies, BRAZIL, NORTH_AFRICA);
		
		// Africa
		createAdjacency(adjacencies, NORTH_AFRICA, WESTERN_EUROPE);
		createAdjacency(adjacencies, NORTH_AFRICA, SOUTHERN_EUROPE);
		createAdjacency(adjacencies, NORTH_AFRICA, EGYPT);
		createAdjacency(adjacencies, NORTH_AFRICA, EAST_AFRICA);
		createAdjacency(adjacencies, NORTH_AFRICA, CENTRAL_AFRICA);
		createAdjacency(adjacencies, EGYPT, SOUTHERN_EUROPE);
		createAdjacency(adjacencies, EGYPT, MIDDLE_EAST);
		createAdjacency(adjacencies, EGYPT, EAST_AFRICA);
		createAdjacency(adjacencies, EAST_AFRICA, MIDDLE_EAST);
		createAdjacency(adjacencies, EAST_AFRICA, CENTRAL_AFRICA);
		createAdjacency(adjacencies, EAST_AFRICA, MADAGASCAR);
		createAdjacency(adjacencies, EAST_AFRICA, SOUTH_AFRICA);
		createAdjacency(adjacencies, MADAGASCAR, SOUTH_AFRICA);
		createAdjacency(adjacencies, SOUTH_AFRICA, CENTRAL_AFRICA);
		
		// Europe
		createAdjacency(adjacencies, WESTERN_EUROPE, GREAT_BRITAIN);
		createAdjacency(adjacencies, WESTERN_EUROPE, NORTHERN_EUROPE);
		createAdjacency(adjacencies, WESTERN_EUROPE, SOUTHERN_EUROPE);
		createAdjacency(adjacencies, GREAT_BRITAIN, ICELAND);
		createAdjacency(adjacencies, GREAT_BRITAIN, SCANDINAVIA);
		createAdjacency(adjacencies, GREAT_BRITAIN, NORTHERN_EUROPE);
		createAdjacency(adjacencies, ICELAND, SCANDINAVIA);
		createAdjacency(adjacencies, SCANDINAVIA, NORTHERN_EUROPE);
		createAdjacency(adjacencies, SCANDINAVIA, EASTERN_EUROPE);
		createAdjacency(adjacencies, NORTHERN_EUROPE, SOUTHERN_EUROPE);
		createAdjacency(adjacencies, NORTHERN_EUROPE, EASTERN_EUROPE);
		createAdjacency(adjacencies, EASTERN_EUROPE, SOUTHERN_EUROPE);
		createAdjacency(adjacencies, EASTERN_EUROPE, URAL);
		createAdjacency(adjacencies, EASTERN_EUROPE, AFGHANISTAN);
		createAdjacency(adjacencies, EASTERN_EUROPE, MIDDLE_EAST);
		createAdjacency(adjacencies, SOUTHERN_EUROPE, MIDDLE_EAST);
		
		// Asia
		createAdjacency(adjacencies, URAL, SIBERIA);
		createAdjacency(adjacencies, URAL, CHINA); // ?
		createAdjacency(adjacencies, URAL, AFGHANISTAN);
		createAdjacency(adjacencies, AFGHANISTAN, CHINA);
		createAdjacency(adjacencies, AFGHANISTAN, INDIA);
		createAdjacency(adjacencies, AFGHANISTAN, MIDDLE_EAST);
		createAdjacency(adjacencies, MIDDLE_EAST, INDIA);
		createAdjacency(adjacencies, SIBERIA, YAKUTSK);
		createAdjacency(adjacencies, SIBERIA, IRKUTSK);
		createAdjacency(adjacencies, SIBERIA, MONGOLIA);
		createAdjacency(adjacencies, SIBERIA, CHINA); // ?
		createAdjacency(adjacencies, CHINA, MONGOLIA);
		createAdjacency(adjacencies, CHINA, INDIA);
		createAdjacency(adjacencies, CHINA, SOUTHEAST_ASIA);
		createAdjacency(adjacencies, SOUTHEAST_ASIA, INDIA);
		createAdjacency(adjacencies, SOUTHEAST_ASIA, INDONESIA);
		createAdjacency(adjacencies, YAKUTSK, IRKUTSK);
		createAdjacency(adjacencies, YAKUTSK, KAMCHATKA);
		createAdjacency(adjacencies, IRKUTSK, KAMCHATKA);
		createAdjacency(adjacencies, IRKUTSK, MONGOLIA);
		createAdjacency(adjacencies, KAMCHATKA, JAPAN);
		createAdjacency(adjacencies, KAMCHATKA, MONGOLIA);
		createAdjacency(adjacencies, MONGOLIA, JAPAN);
		
		// Australia
		createAdjacency(adjacencies, INDONESIA, NEW_GUINEA);
		createAdjacency(adjacencies, INDONESIA, WESTERN_AUSTRALIA);
		createAdjacency(adjacencies, NEW_GUINEA, WESTERN_AUSTRALIA);
		createAdjacency(adjacencies, NEW_GUINEA, EASTERN_AUSTRALIA);
		createAdjacency(adjacencies, WESTERN_AUSTRALIA, EASTERN_AUSTRALIA);
		
		return adjacencies;
		
	}
	
	private static void createAdjacency(long[] adjacencies, Territory a, Territory b) {
		
		adjacencies[a.ID] |= b.MASK;
		adjacencies[b.ID] |= a.MASK;
		
	}
	
	public static Territory fromID(int id) {
		return VALUES[id];
	}
	
	public static EnumSet<Territory> maskToSet(long mask) {
		
		mask &= Territory.ALL_TERRITORIES_MASK;
		
		EnumSet<Territory> set = EnumSet.noneOf(Territory.class);
		
		// Go through each set bit, single it out and convert it to an index
		while(mask != 0L) {
			
			long bitRemoved = mask & (mask - 1L);
			long bitAlone = mask ^ bitRemoved;
			
			mask = bitRemoved;
			
			set.add(VALUES[Util.log2OfPowerOf2(bitAlone)]);
			
		}
		
		return set;
		
	}
	
	public static long setToMask(EnumSet<Territory> set) {
		
		long mask = 0L;
		
		for(Territory territory : set)
			mask |= territory.MASK;
		
		return mask;
		
	}
	
	@Override
	public String toString() {
		return NAME;
	}
	
}
