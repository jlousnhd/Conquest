package conquest;

import java.io.*;
import java.net.*;

public class Program {

	public static void main(String[] args) throws IOException {
		
		if(args.length == 0 || args.length == 2) {
			
			System.out.println("Server Usage:\n\tjava Program <port>\n");
			System.out.println("Client Usage:\n\tjava Program <host> <port> <username>");
			
			return;
			
		}
		
		if(args.length == 1) {
			
			int port = Integer.parseInt(args[0]);
			
			ServerInstance server = new ServerInstance(port);
			server.start();
			
		}
		
		else {
			
			GameMap.loadImages();
			
			int port = Integer.parseInt(args[1]);
			
			ClientInstance client = new ClientInstance(InetAddress.getByName(args[0]), port, args[2]);
			client.start();
			
		}
		
	}

}
