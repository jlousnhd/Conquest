package conquest;

public final class MessageType {
	
	private MessageType() {}
	
	public static final int HELLO           = 0x00;
	public static final int PLAYERS         = 0x01;
	public static final int GAME_STATE      = 0x02;
	public static final int PLACE_ARMY      = 0x03;
	public static final int EXCHANGE_CARDS  = 0x04;
	public static final int ATTACK          = 0x05;
	public static final int TRANSFER_ARMIES = 0x06;
	public static final int END_TURN        = 0x07;
	public static final int GAME_FAIL       = 0x08;
	public static final int CHAT            = 0x09;
	
}
