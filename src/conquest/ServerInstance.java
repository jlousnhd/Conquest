package conquest;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public final class ServerInstance {
	
	public static final int MAX_CLIENTS = 15;
	public static final int MAX_PLAYERS = 6;
	public static final int MIN_PLAYERS = 2;
	
	private final Thread listeningThread;
	private final ServerSocket serverSocket;
	private final TreeMap<Integer, ClientConnection> clients;
	private final TreeSet<Integer> idPool;
	
	private final ServerWindow serverWindow;
	
	private volatile boolean startGame;
	private boolean hasQuit;
	private boolean serverStarted;
	
	private GameData data;
	
	public ServerInstance(int port) throws IOException {
		
		this.idPool = new TreeSet<Integer>();
		
		for(int i = 1; i <= MAX_CLIENTS; ++i)
			this.idPool.add(i);
			
		
		this.serverSocket = new ServerSocket(port);
		this.listeningThread = new Thread(new Listener());
		this.clients = new TreeMap<Integer, ClientConnection>();
		
		this.serverWindow = new ServerWindow();
		this.serverWindow.setVisible(true);
		
	}
	
	public void start() {
		
		if(serverStarted)
			return;
		
		serverStarted = true;
		listeningThread.start();
		
	}
	
	private void kill() {
		
		try {
			serverSocket.close();
		} catch(Exception e) {}
		
		synchronized(clients) {
			
			for(ClientConnection client : clients.values())
				client.kill();
			
			clients.clear();
			
		}
		
	}
	
	public boolean startGame() {
		
		synchronized(clients) {
			
			if(!startGame && clients.size() >= MIN_PLAYERS && clients.size() <= MAX_PLAYERS) {
				
				startGame = true;
				listeningThread.interrupt();
				
				data = new GameData(clients.keySet());
				data.initializeGame();
				
				broadcastGameState();
				
				return true;
				
			}
			
		}
		
		return false;
		
	}
	
	private void broadcastGameState() {
		
		synchronized(clients) {
			
			for(ClientConnection client : clients.values()) {
				
				byte[] message = data.toBytes(client.id);
				client.sendMessage(message);
				
			}
			
		}
		
	}
	
	private void broadcastPlayerList() {
		
		synchronized(clients) {
			
			TreeMap<Integer, String> players = new TreeMap<Integer, String>();
			
			for(Map.Entry<Integer, ClientConnection> entry : clients.entrySet())
				players.put(entry.getKey(), entry.getValue().name);
			
			Players p = new Players(players);
			byte[] message = p.toBytes(null);
			
			for(ClientConnection client : clients.values())
				client.sendMessage(message);
			
		}
		
	}
	
	private void broadcastChat(Chat chat) {
		
		synchronized(clients) {
			
			byte[] message = chat.toBytes(null);
			
			for(ClientConnection client : clients.values())
				client.sendMessage(message);
			
		}
		
	}
	
	private final class Listener implements Runnable {

		@Override
		public void run() {
			
			try {
				
				log("Listening on port " + serverSocket.getLocalPort() + "...");
				
				for(;;) {
					
					Socket socket = serverSocket.accept();
					
					synchronized(clients) {
						
						new ClientConnection(socket).start();
						
					}
					
				}
				
			} catch(Exception e) {
				
				if(!startGame) {
					
					log("Error listening for clients:  " + e.getMessage());
					kill();
					
				}
				
			}
			
		}
		
	}
	
	public boolean hasStartedGame() {
		
		synchronized(clients) {
			return data != null;
		}
		
	}
	
	private void cancelGameIfStarted(String reason) {
		
		synchronized(clients) {
			if(hasStartedGame())
				quit(reason);
		}
		
	}
	
	public void quit(String reason) {
		
		reason = reason == null ? "No reason given" : reason;
		
		synchronized(clients) {
			
			if(!hasStartedGame())
				return;
			
			if(hasQuit)
				return;
			
			hasQuit = true;
			
			for(ClientConnection client : clients.values())
				client.quit(reason);
			
		}
		
	}
	
	public void kick(Integer id) {
		
		synchronized(clients) {
			
			if(!serverStarted || hasQuit || !clients.containsKey(id))
				return;
			
			ClientConnection client = clients.get(id);
			client.kill();
			
		}
		
	}
	
	private final class ClientConnection {
		
		private static final int BUFFER_SIZE = 512;
		private static final int HEADER_LENGTH = 2;
		private static final int MESSAGE_TYPE_OFFSET = 0;
		private static final int MESSAGE_LENGTH_OFFSET = 1;
		
		private final Thread readThread, writeThread;
		
		private final Socket socket;
		private final Integer id;
		private final Queue<byte[]> pendingOut;
		private volatile String name;
		private volatile boolean quit;
		
		private boolean helloReceived;
		
		public ClientConnection(Socket socket) {
			
			synchronized(clients) {
				
				id = idPool.pollFirst();
				
				if(id == null)
					throw new IllegalStateException("ID pool exhausted (too many clients connected?)");
				
				this.socket = socket;
				this.pendingOut = new LinkedList<byte[]>();
				
				this.name = "Player " + id;
				clients.put(id, this);
				
			}
			
			readThread = new Thread(new Reader());
			writeThread = new Thread(new Writer());
			
		}
		
		private void start() {
			
			readThread.start();
			writeThread.start();
			
			Hello hello = new Hello(name, id);
			sendMessage(hello.toBytes(id));
			
		}
		
		private void kill() {
			
			synchronized(clients) {
				
				quit = true;
				
				idPool.add(id);
				clients.remove(id);
				
				try {
					socket.close();
				} catch(Exception e) {}
				
				// If game has started, cancel it
				cancelGameIfStarted("Player " + name + " was disconnected");
				
			}
			
		}
		
		private final class Reader implements Runnable {

			@Override
			public void run() {
				
				byte[] buffer = new byte[BUFFER_SIZE];
				
				try {
					
					InputStream is = socket.getInputStream();
					int offset = 0;
					
					for(;;) {
						
						if(quit)
							return;
						
						int read = is.read(buffer, offset, buffer.length - offset);
						
						if(read < 0)
							throw new IOException("Input stream was closed");
						
						offset += read;
						
						// Do we have a header?
						while(offset >= HEADER_LENGTH) {
							
							if(quit)
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
					
					log("Error reading from server:  " + e.getMessage());
					kill();
					
				}
				
			}
			
		}
		
		private final class Writer implements Runnable {

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
					
					log("Error writing to client:  " + e.getMessage());
					kill();
					
				}
				
			}
			
		}
		
		public void decodeMessage(byte[] message, int length) {
			
			switch((int) message[MESSAGE_TYPE_OFFSET]) {
			
			case MessageType.HELLO:
				onHello(Hello.fromBytes(message, length));
				break;
				
			case MessageType.PLACE_ARMY:
				onPlaceArmies(PlaceArmies.fromBytes(message, length));
				break;
				
			case MessageType.EXCHANGE_CARDS:
				onExchangeCards(ExchangeCards.fromBytes(message, length));
				break;
				
			case MessageType.ATTACK:
				onAttack(Attack.fromBytes(message, length));
				break;
				
			case MessageType.TRANSFER_ARMIES:
				onTransferArmies(TransferArmies.fromBytes(message, length));
				break;
				
			case MessageType.END_TURN:
				onEndTurn(EndTurn.fromBytes(message, length));
				break;
				
			case MessageType.CHAT:
				onChat(Chat.fromBytes(message, length));
				break;
			
			}
			
		}
		
		public void quit(String reason) {
			
			if(quit)
				return;
			
			GameFail fail = new GameFail(reason);
			sendMessage(fail.toBytes(null));
			quit = true;
			
		}
		
		public void sendMessage(byte[] message) {
			
			synchronized(pendingOut) {
				
				if(quit)
					return;
				
				pendingOut.add(message);
				pendingOut.notifyAll();
				
			}
			
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		private void onHello(Hello hello) {
			
			if(helloReceived) {
				
				log("Received hello packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(hello == null || !id.equals(hello.ID)) {
				
				log("Received bad hello packet from " + name);
				kill();
				return;
				
			}
			
			helloReceived = true;
			
			synchronized(clients) {
				
				name = hello.NAME;
				log(name + " connected to the server");
				updateClientList();
				
			}
			
			broadcastPlayerList();
			
		}
		
		private void onPlaceArmies(PlaceArmies placeArmies) {
			
			if(!helloReceived || !hasStartedGame()) {
				
				log("Received place army packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(placeArmies == null) {
				
				log("Received bad place armies packet from " + name);
				kill();
				return;
				
			}
			
			if(!data.doMove(id, placeArmies)) {
				
				log("Received place armies packet at bad time from " + name);
				kill();
				return;
				
			}
			
			broadcastGameState();
			
		}
		
		private void onExchangeCards(ExchangeCards exchangeCards) {
			
			if(!helloReceived || !hasStartedGame()) {
				
				log("Received exchange cards packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(exchangeCards == null) {
				
				log("Received bad exchange cards packet from " + name);
				kill();
				return;
				
			}
			
			if(!data.doMove(id, exchangeCards)) {
				
				log("Received exchange cards packet at bad time from " + name);
				kill();
				return;
				
			}
			
			broadcastGameState();
			
		}
		
		private void onAttack(Attack attack) {
			
			if(!helloReceived || !hasStartedGame()) {
				
				log("Received attack packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(attack == null) {
				
				log("Received bad attack packet from " + name);
				kill();
				return;
				
			}
			
			if(!data.doMove(id, attack)) {
				
				log("Received attack packet at bad time from " + name);
				kill();
				return;
				
			}
			
			broadcastGameState();
			
		}
		
		private void onTransferArmies(TransferArmies transferArmies) {
			
			if(!helloReceived || !hasStartedGame()) {
				
				log("Received transfer armies packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(transferArmies == null) {
				
				log("Received bad transfer armies packet from " + name);
				kill();
				return;
				
			}
			
			if(!data.doMove(id, transferArmies)) {
				
				log("Received transfer armies packet at bad time from " + name);
				kill();
				return;
				
			}
			
			broadcastGameState();
			
		}
		
		private void onEndTurn(EndTurn endTurn) {
			
			if(!helloReceived || !hasStartedGame()) {
				
				log("Received end turn packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(endTurn == null) {
				
				log("Received bad end turn packet from " + name);
				kill();
				return;
				
			}
			
			if(!data.doMove(id, endTurn)) {
				
				log("Received end turn packet at bad time from " + name);
				kill();
				return;
				
			}
			
			broadcastGameState();
			
		}
		
		private void onChat(Chat chat) {
			
			if(!helloReceived) {
				
				log("Received chat packet at wrong time from " + name);
				kill();
				return;
				
			}
			
			if(!chat.getPlayerID().equals(id)) {
				
				log("Received bad chat packet from " + name);
				kill();
				return;
				
			}
			
			broadcastChat(chat);
			
		}
		
	}
	
	private void log(String message) {
		SwingUtilities.invokeLater(new LogUpdater(message));
	}
	
	private void updateClientList() {
		SwingUtilities.invokeLater(serverWindow);
	}
	
	private final class LogUpdater implements Runnable {
		
		private final String log;
		
		public LogUpdater(String log) {
			this.log = log;
		}
		
		public void run() {
			serverWindow.log(log);
		}
		
	}
	
	private final class ServerWindow extends JFrame implements ActionListener, WindowListener, ListSelectionListener, Runnable {
		
		private static final long serialVersionUID = 1L;
		
		private final DefaultListModel<String> logModel;
		private final DefaultListModel<ClientConnection> clientModel;
		private final JButton start, kick, quit;
		private final JList<ClientConnection> clientList;
		
		public ServerWindow() {
			
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			this.addWindowListener(this);
			
			logModel = new DefaultListModel<String>();
			clientModel = new DefaultListModel<ClientConnection>();
			
			Box mainBox = Box.createHorizontalBox();
			
			Box controlBox = Box.createVerticalBox();
			mainBox.add(controlBox);
			
			JScrollPane logPane = new JScrollPane(new JList<String>(logModel));
			controlBox.add(logPane);
			
			start = new JButton("Start Game");
			start.addActionListener(this);
			controlBox.add(start);
			
			quit = new JButton("Quit");
			quit.addActionListener(this);
			controlBox.add(quit);
			
			Box clientBox = Box.createVerticalBox();
			mainBox.add(clientBox);
			
			clientList = new JList<ClientConnection>(clientModel);
			clientList.addListSelectionListener(this);
			
			JScrollPane clientPane = new JScrollPane(clientList);
			clientBox.add(clientPane);
			
			kick = new JButton("Kick");
			kick.addActionListener(this);
			clientBox.add(kick);
			
			setContentPane(mainBox);
			this.setSize(640, 480);
			
			run();
			valueChanged(null);
			
		}
		
		public void log(String log) {
			logModel.add(logModel.getSize(), log);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource() == start) {
				startGame();
			}
			
			else if(e.getSource() == quit) {
				kill();
				dispose();
			}
			
			else if(e.getSource() == kick) {
				
				ClientConnection client = clientList.getSelectedValue();
				
				if(client != null )
					kick(client.id);
				
			}
			
		}

		@Override
		public void windowClosed(WindowEvent e) {
			quit("Server admin shut down server");
		}
		
		@Override
		public void windowActivated(WindowEvent e) {}

		@Override
		public void windowClosing(WindowEvent e) {}

		@Override
		public void windowDeactivated(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowOpened(WindowEvent e) {}

		@Override
		public void run() {
			
			// Update buttons, clients
			synchronized(clients) {
				
				start.setEnabled(clients.size() >= 2 && clients.size() <= 6);
				
				clientModel.clear();
				clientModel.addAll(clients.values());
				
			}
			
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			kick.setEnabled(clientList.getSelectedValue() != null);
		}
		
	}

}
