package conquest;
import java.util.*;

public enum Continent {
	
	NORTH_AMERICA(5,
		Territory.ALASKA,
		Territory.WESTERN_CANADA,
		Territory.CENTRAL_AMERICA,
		Territory.EASTERN_US,
		Territory.GREENLAND,
		Territory.NORTHWEST_TERRITORY,
		Territory.CENTRAL_CANADA,
		Territory.EASTERN_CANADA,
		Territory.WESTERN_US
	),
	
	SOUTH_AMERICA(2,
		Territory.ARGENTINA,
		Territory.BRAZIL,
		Territory.PERU,
		Territory.VENEZUELA
	),
	
	AFRICA(3,
		Territory.CENTRAL_AFRICA,
		Territory.EAST_AFRICA,
		Territory.EGYPT,
		Territory.MADAGASCAR,
		Territory.NORTH_AFRICA,
		Territory.SOUTH_AFRICA
	),
	
	EUROPE(5,
		Territory.GREAT_BRITAIN,
		Territory.ICELAND,
		Territory.NORTHERN_EUROPE,
		Territory.SCANDINAVIA,
		Territory.SOUTHERN_EUROPE,
		Territory.EASTERN_EUROPE,
		Territory.WESTERN_EUROPE
	),
	
	ASIA(7,
		Territory.AFGHANISTAN,
		Territory.CHINA,
		Territory.INDIA,
		Territory.IRKUTSK,
		Territory.JAPAN,
		Territory.KAMCHATKA,
		Territory.MIDDLE_EAST,
		Territory.MONGOLIA,
		Territory.SOUTHEAST_ASIA,
		Territory.SIBERIA,
		Territory.URAL,
		Territory.YAKUTSK
	),
	
	AUSTRALIA(2,
		Territory.EASTERN_AUSTRALIA,
		Territory.INDONESIA,
		Territory.NEW_GUINEA,
		Territory.WESTERN_AUSTRALIA
	);
	
	public static final List<Continent> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	
	public final int ARMY_BONUS;
	public final long MASK;
	
	Continent(int armyBonus, Territory... territories) {
		
		long mask = 0L;
		
		for(Territory territory : territories)
			mask |= territory.MASK;
		
		this.ARMY_BONUS = armyBonus;
		this.MASK = mask;
		
	}
	
}
