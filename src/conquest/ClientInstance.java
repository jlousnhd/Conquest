package conquest;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public final class ClientInstance {
	
	private static final int BUFFER_SIZE = 512;
	private static final int HEADER_LENGTH = 2;
	private static final int MESSAGE_TYPE_OFFSET = 0;
	private static final int MESSAGE_LENGTH_OFFSET = 1;
	
	private final Socket socket;
	private final Queue<byte[]> pendingOut;
	private final Thread readThread, writeThread;
	private final String name;
	
	private volatile boolean kill;
	
	private volatile int myID;
	private volatile TreeMap<Integer, String> playerIDsNames;
	private volatile GameData data;
	private volatile ClientWindow window;
	
	private Territory lastAttacker, lastDefender;
	
	public ClientInstance(InetAddress address, int port, String name) throws IOException {
		
		this.pendingOut = new LinkedList<byte[]>();
		
		this.socket = new Socket(address, port);
		this.name = name;
		
		this.readThread = new Thread(new Reader());
		this.writeThread = new Thread(new Writer());
		
		this.window = new ClientWindow();
		
	}
	
	private void log(String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.log(message);
			}
		});
	}
	
	public void start() {
		
		readThread.start();
		writeThread.start();
		
	}
	
	private void kill(String reason) {
		
		if(!kill)
			return;
		
		kill = true;
		log("Client killed:  " + reason);
		
		readThread.interrupt();
		writeThread.interrupt();
		
	}
	
	private void sendMessage(byte[] message) {
		
		synchronized(pendingOut) {
			
			pendingOut.add(message);
			pendingOut.notifyAll();
			
		}
		
	}
	
	private void onHello(Hello hello) {
		
		myID = hello.ID;
		
		hello = new Hello(name, myID);
		sendMessage(hello.toBytes(myID));
		
	}
	
	private void onPlayers(Players players) {
		
		playerIDsNames = new TreeMap<Integer, String>(players.PLAYERS);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.playerModel.clear();
				window.playerModel.addAll(playerIDsNames.values());
			}
		});
		
	}
	
	private void onGameState(GameData data) {
		
		this.data = data;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.updateFromData(ClientInstance.this.data);
			}
		});
		
	}
	
	private void onGameFail(GameFail fail) {
		kill("Session terminated by server:  " + fail.getReason());
	}
	
	private void onChat(Chat chat, Map<Integer, String> playerIDsNames) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.log("<" + playerIDsNames.get(chat.getPlayerID()) + ">: " + chat.getChat());
			}
		});
	}
	
	private final class Reader implements Runnable {
		
		//private Map<Integer, String> playerIDsNames;
		private GameData data;
		
		@Override
		public void run() {
			
			byte[] buffer = new byte[BUFFER_SIZE];
			
			try {
				
				InputStream is = socket.getInputStream();
				int offset = 0;
				
				for(;;) {
					
					if(kill)
						return;
					
					int read = is.read(buffer, offset, buffer.length - offset);
					
					if(read < 0)
						throw new IOException("Input stream was closed");
					
					offset += read;
					
					// Do we have a header?
					while(offset >= HEADER_LENGTH) {
						
						if(kill)
							return;
						
						int length = (buffer[MESSAGE_LENGTH_OFFSET] & 0xff) + HEADER_LENGTH;
						
						// Do we have a full message?
						if(offset >= length) {
							
							decodeMessage(buffer, length);
							
							System.arraycopy(buffer, length, buffer, 0, offset - length);
							offset -= length;
							
						}
					
					}
			
				}
				
			} catch(Exception e) {
				kill("Error reading from server:  " + e.getMessage());
			}
		
		}
		
		public void decodeMessage(byte[] message, int length) throws IOException {
			
			switch((int) message[MESSAGE_TYPE_OFFSET]) {
			
			case MessageType.HELLO:
				onHello(Hello.fromBytes(message, length));
				break;
				
			case MessageType.PLAYERS:
				Players players = Players.fromBytes(message, length);
				playerIDsNames = new TreeMap<Integer, String>(players.PLAYERS);
				onPlayers(players);
				break;
				
			case MessageType.GAME_STATE:
				data = GameData.fromBytes(message, length, playerIDsNames.keySet(), myID);
				onGameState(data);
				break;
				
			case MessageType.GAME_FAIL:
				onGameFail(GameFail.fromBytes(message, length));
				break;
				
			case MessageType.CHAT:
				onChat(Chat.fromBytes(message, length), playerIDsNames);
				break;
			
			}
			
		}
		
	}
	
	private class Writer implements Runnable {

		@Override
		public void run() {
			
			try {
				
				OutputStream os = socket.getOutputStream();
				
				synchronized(pendingOut) {
					
					for(;;) {
						
						byte[] message;
						
						while((message = pendingOut.poll()) != null)
							os.write(message);
						
						pendingOut.wait();
						
					}
					
				}
				
			} catch(Exception e) {
				kill("Error writing to server:  " + e.getMessage());
			}
			
		}
		
	}
	
	private class MapView extends GameMap {

		private static final long serialVersionUID = 1L;
		
		public MapView() {
			super(null);
		}
		
		@Override
		public boolean territoryClicked(Territory territory, GameData data, GamePhase phase) {
			
			if(data.getPlayerTurn().getID() != myID)
				return false;
			
			if(phase == GamePhase.CLAIMING) {
				
				PlayerData owner = data.territoryOwner(territory);
				
				if(owner == null || (owner.getID() == myID && data.areAllTerritoriesOwned())) {
					
					// Claim territory
					PlaceArmies placeArmies = new PlaceArmies(territory, 1);
					lastAttacker = lastDefender = null;
					sendMessage(placeArmies.toBytes(myID));
					
					return true;
					
				}
				
				return false;
				
				
			} else if(phase == GamePhase.PLACEMENT) {
				
				if(!data.playerOwnsTerritory(myID, territory))
					return false;
				
				// Place armies
				int freeArmies = data.getPlayerTurn().getFreeArmies();
				
				int armies = window.promptForNumber("How many armies to place on " + territory + "?", 1, freeArmies);
				
				if(armies > 0) {
					
					PlaceArmies placeArmies = new PlaceArmies(territory, armies);
					lastAttacker = lastDefender = null;
					sendMessage(placeArmies.toBytes(myID));
					
					return true;
					
				}
				
			}
			
			return false;
			
		}
		
		@Override
		public boolean territoryClicked(Territory first, Territory second, GameData data, GamePhase phase) {
			
			if(data.getPlayerTurn().getID() != myID)
				return false;
			
			if(phase == GamePhase.ATTACKING) {
				
				// If we don't own first territory, we're can't do anything
				if(!data.playerOwnsTerritory(myID, first))
					return false;
				
				// If enemy owns second territory, attack
				if(!data.playerOwnsTerritory(myID, second)) {
					
					int armies = data.territoryArmies(first);
					
					// We can't attack with 1 army
					if(armies <= 1)
						return true;
					
					Attack attack = new Attack(first, second, Math.min(3, armies - 1));
					lastAttacker = first;
					lastDefender = second;
					sendMessage(attack.toBytes(myID));
					return true;
					
				}
				
				// If we own both territories
				else {
					
					int armies = data.territoryArmies(first);
					
					// We can't transfer with 1 army
					if(armies <= 1)
						return false;
					
					// Can we transfer armies to newly conquered territory?
					if(first == lastAttacker && second == lastDefender) {
						
						// How many armies to transfer?
						armies = window.promptForNumber("How many armies to transfer from " + first + " to " + second + "?", 1, armies - 1);
						
						if(armies > 0) {
							
							TransferArmies transferArmies = new TransferArmies(first, second, armies);
							lastAttacker = lastDefender = null;
							sendMessage(transferArmies.toBytes(myID));
							return true;
							
						}
						
					}
					
					// Shall we end turn?
					else {
						
						// How many armies to transfer?
						armies = window.promptForNumber("How many armies to transfer from " + first + " to " + second + "?  (This will end your turn.)", 1, armies - 1);
						
						if(armies > 0) {
							
							EndTurn endTurn = new EndTurn(first, second, armies);
							lastAttacker = lastDefender = null;
							sendMessage(endTurn.toBytes(myID));
							return true;
							
						}
						
					}
					
				}
				
			}
			
			return false;
			
		}
		
	}
	
	private final class ClientWindow extends JFrame implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		private final JLabel instructionLabel;
		private final MapView map;
		private final DefaultListModel<String> chatModel;
		private final DefaultListModel<String> playerModel;
		
		private final JTextField textField;
		private final JButton exchangeCards;
		private final JButton endTurn;
		private final JLabel cardLabel;
		
		public ClientWindow() {
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			instructionLabel = new JLabel();
			map = new MapView();
			chatModel = new DefaultListModel<String>();
			playerModel = new DefaultListModel<String>();
			
			textField = new JTextField();
			textField.addActionListener(this);
			
			exchangeCards = new JButton("Exchange Cards");
			exchangeCards.addActionListener(this);
			
			endTurn = new JButton("End Turn");
			endTurn.addActionListener(this);
			
			cardLabel = new JLabel();
			
			Box mainBox = Box.createVerticalBox();
			mainBox.add(instructionLabel);
			mainBox.add(map);
			
			Box bottomBox = Box.createHorizontalBox();
			bottomBox.add(new JLabel("Chat: "));
			bottomBox.add(textField);
			bottomBox.add(cardLabel);
			bottomBox.add(exchangeCards);
			bottomBox.add(endTurn);
			
			mainBox.add(bottomBox);
			
			Box sideBox = Box.createVerticalBox();
			sideBox.add(new JList<String>(playerModel));
			sideBox.add(new JScrollPane(new JList<String>(chatModel)));
			
			Box mainPanel = Box.createHorizontalBox();
			mainPanel.add(mainBox);
			mainPanel.add(sideBox);
			
			updateFromData(null);
			setContentPane(mainPanel);
			pack();
			setVisible(true);
			
		}
		
		public void updateFromData(GameData data) {
			
			map.setGameData(data);
			
			if(data == null) {
				
				instructionLabel.setText("Waiting to start game...");
				exchangeCards.setEnabled(false);
				endTurn.setEnabled(false);
				map.setEnabled(false);
				
				return;
				
			}
			
			GamePhase phase = data.phase();
			
			if(phase == GamePhase.VICTORY) {
				
				PlayerData winner = data.winner();
				
				if(winner.getID().equals(myID))
					instructionLabel.setText("You won!");
				
				else
					instructionLabel.setText(playerIDsNames.get(winner.getID()) + " won");
				
				exchangeCards.setEnabled(false);
				endTurn.setEnabled(false);
				map.setEnabled(false);
				
				return;
				
			}
			
			PlayerData currentPlayer = data.getPlayerTurn();
			Integer currentPlayerID = currentPlayer.getID();
			
			if(currentPlayerID != myID) {
				
				instructionLabel.setText(playerIDsNames.get(currentPlayerID) + "'s turn...");
				exchangeCards.setEnabled(false);
				endTurn.setEnabled(false);
				map.setEnabled(false);
				
				return;
				
			}
			
			// My turn
			
			map.setEnabled(true);
			
			if(phase == GamePhase.CLAIMING) {
				
				if(data.areAllTerritoriesOwned())
					instructionLabel.setText("Your turn, fortify a territory");
				
				else
					instructionLabel.setText("Your turn, claim an unowned territory");
				
				exchangeCards.setEnabled(false);
				endTurn.setEnabled(false);
				
			} else if(phase == GamePhase.PLACEMENT) {
				
				instructionLabel.setText("Your turn, place " + currentPlayer.getFreeArmies() + " armies");
				exchangeCards.setEnabled(Card.containsExchangeable(currentPlayer.getOwnedCards()));
				endTurn.setEnabled(false);
				
			} else if(phase == GamePhase.ATTACKING) {
				
				instructionLabel.setText("Your turn, attack");
				exchangeCards.setEnabled(Card.containsExchangeable(currentPlayer.getOwnedCards()));
				endTurn.setEnabled(true);
				
			}
			
		}
		
		public int promptForNumber(String prompt, int min, int max) {
			
			String answer = JOptionPane.showInputDialog(this, prompt);
			
			try {
				
				int num = Integer.parseInt(answer);
				
				if(num >= min && num <= max)
					return num;
				
			} catch(Exception e) {}
			
			return -1;
			
		}
		
		public void log(String log) {
			chatModel.add(chatModel.getSize(), log);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			GameData data = ClientInstance.this.data;
			
			// Chat box
			if(e.getSource() == textField) {
				
				String text = textField.getText();
				
				if(text.length() > 1 && text.length() <= 254) {
					
					Chat chat = new Chat(myID, text);
					sendMessage(chat.toBytes(myID));
					
					textField.setText("");
					
				}
				
			}
			
			// Buttons
			else {
				
				if(data == null)
					return;
				
				GamePhase phase = data.phase();
				
				if(e.getSource() == endTurn) {
					
					if(data.getPlayerTurn().getID().equals(myID) && phase == GamePhase.ATTACKING) {
						
						EndTurn endTurn = new EndTurn(null, null, 0);
						lastAttacker = lastDefender = null;
						sendMessage(endTurn.toBytes(myID));
						
					}
					
				} else if(e.getSource() == exchangeCards) {
					
					if(data.getPlayerTurn().getID().equals(myID) && phase == GamePhase.ATTACKING) {
						
						EnumSet<Card> cards = data.getPlayerTurn().getOwnedCards();
						
						if(Card.containsExchangeable(cards)) {
							
							EnumSet<Card> copy = cards.clone();
							
							for(Card card : copy) {
								
								cards.remove(card);
								
								if(!Card.containsExchangeable(cards))
									cards.add(card);
								
								else
									continue;
								
							}
							
							assert(cards.size() == 3);
							
							ExchangeCards exchangeCards = new ExchangeCards(cards);
							sendMessage(exchangeCards.toBytes(myID));
							
						}
						
					}
					
				}
				
			}
			
			return;
			
		}
		
	}

}
