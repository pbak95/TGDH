package sources;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;

import sources.Tree.NodeType;

import java.net.ServerSocket;

/*
 * A chat server that delivers public and private messages.
 */
public class ChatServer implements Runnable {

	// Server reference
	private static ChatServer server;
	// The server socket for broadcast.
	private ServerSocket serverSocket1 = null;
	// The server socket for signalization commands.
	private ServerSocket serverSocket2 = null;
	// The server socket for signalization objects.
	private ServerSocket serverSocket3 = null;
	// The client socket for messages.
	private Socket clientSocket1 = null;
	// The client socket2 for signaling commands.
	private Socket clientSocket2 = null;
	// The client socket3 for signaling objects.
	private Socket clientSocket3 = null;
	// Tree structure of users
	public Tree tree;
	// Broadcast port
	private final int b_port_number = 2222;
	// Signaling port for commands
	private static int s_c_port_number = 13389;
	// Signaling port for objects
	private static int s_o_port_number = 13390;

	// This chat server can accept up to maxClientsCount clients' connections.
	private static final int maxClientsCount = 10;
	private final clientThread[] threads = new clientThread[maxClientsCount];
	// Client container
	private Vector<ClientService> clients = new Vector<ClientService>();
	// PUBLIC VALUES
	public BigInteger p;
	public BigInteger g;

	public static int clients_number_on_level_2 = 0;
	public Node rootHandler;
	public int calculatedBranches = 0;
	public int readyleafs = 0;
	

	public ChatServer() {

		System.out.println(
				"Usage: java MultiThreadChatServerSync <portNumber>\n" + "Now using port number=" + b_port_number);

		try {
			this.serverSocket1 = new ServerSocket(b_port_number);
			this.serverSocket2 = new ServerSocket(s_c_port_number);
			this.serverSocket3 = new ServerSocket(s_o_port_number);
		} catch (IOException e) {
			System.err.println("Error starting Chat Server.");
			System.exit(1);
		}

		//Initialize tree

		this.tree = new Tree();
		this.p = this.tree.getP();
		this.g = this.tree.getG();
		this.initializeTreeFor4Users();


		new Thread(this).start();
	}
	
	public void run() {

		while (true) {
			try {

				this.clientSocket1 = this.serverSocket1.accept();
				this.clientSocket2 = this.serverSocket2.accept();
				this.clientSocket3 = this.serverSocket3.accept();
				//System.out.println("New session started");
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (this.threads[i] == null) {
						(this.threads[i] = new clientThread(this.clientSocket1, this.threads, i)).start();
						ClientService clientService = new ClientService(this.clientSocket2, this.clientSocket3, server,
								this.clients.size());
						addClientService(clientService);
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(this.clientSocket1.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					this.clientSocket1.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}

	}

	synchronized void addClientService(ClientService clientService) throws IOException {
		clientService.init();
		this.clients.addElement(clientService);
		if (clients_number_on_level_2 == 0) {
			Node node = new Node(clientService, 2, clients_number_on_level_2, tree.getG(), tree.getP(), "Patryk",
					NodeType.Type.LEAFNODE, rootHandler.left, NodeType.Position.LEFT);
			rootHandler.left.left = node;
			this.tree.addNewNode(node);
			clientService.setL(2);
			clientService.setV(0);

		} else if (clients_number_on_level_2 == 1) {
			Node node = new Node(clientService, 2, clients_number_on_level_2, tree.getG(), tree.getP(), "Michal",
					NodeType.Type.LEAFNODE, rootHandler.left, NodeType.Position.RIGHT);
			rootHandler.left.right = node;
			this.tree.addNewNode(node);
			clientService.setL(2);
			clientService.setV(1);

		} else if (clients_number_on_level_2 == 2) {
			Node node = new Node(clientService, 2, clients_number_on_level_2, tree.getG(), tree.getP(), "Tomek",
					NodeType.Type.LEAFNODE, rootHandler.right, NodeType.Position.LEFT);
			rootHandler.right.left = node;
			this.tree.addNewNode(node);
			clientService.setL(2);
			clientService.setV(2);

		} else if (clients_number_on_level_2 == 3) {
			Node node = new Node(clientService, 2, clients_number_on_level_2, tree.getG(), tree.getP(), "Marek",
					NodeType.Type.LEAFNODE, rootHandler.right, NodeType.Position.RIGHT);
			rootHandler.right.right = node;
			this.tree.addNewNode(node);
			clientService.setL(2);
			clientService.setV(3);

		}
		clients_number_on_level_2++;
		new Thread(clientService).start();
		// clientService.send(Protocol.SETPOSITION);
		System.out.println("\nAdd. " + clients.size());
		

	}
	
	public void checkLeafs(){
		if(this.readyleafs == 2){
			this.leftBranchLeafBKDistribution();
		} else if(this.readyleafs ==4){
			this.rightBranchLeafBKDistribution();
			this.readyleafs = 0;
		}else{
			//do nothing
		}
	}
	
	public void leftBranchLeafBKDistribution(){
		this.rootHandler.left.right.clientServiceHandler.send(Protocol.INITIALIZEBKDISTRIBUTION);
		
	}
	
	public void rightBranchLeafBKDistribution(){
		this.rootHandler.right.right.clientServiceHandler.send(Protocol.INITIALIZEBKDISTRIBUTION);
	}
	
	public void parentDistributionRight() {
		this.rootHandler.right.left.clientServiceHandler.send(Protocol.OTHERPARENT);
		this.rootHandler.right.right.clientServiceHandler.send(Protocol.OTHERPARENT);
	}
	
	public void parentDistributionLeft() {
		this.rootHandler.left.left.clientServiceHandler.send(Protocol.OTHERPARENT);
		this.rootHandler.left.right.clientServiceHandler.send(Protocol.OTHERPARENT);
	}

	synchronized void removeClientService(ClientService clientService) {
		clients.removeElement(clientService);
		clientService.close();
		System.out.println("\nRemove. " + clients.size());
		clients_number_on_level_2--;
	}

	synchronized void send(String msg) {
		Enumeration<ClientService> e = clients.elements();
		while (e.hasMoreElements())
			((ClientService) e.nextElement()).send(msg);
	}

	synchronized void send(String msg, ClientService skip) {
		Enumeration<ClientService> e = clients.elements();
		while (e.hasMoreElements()) {
			ClientService elem = (ClientService) e.nextElement();
			if (elem != skip)
				elem.send(msg);
		}
	}

	private void initializeTreeFor4Users() {
		this.rootHandler = this.tree.getByPosition(0, 0, this.tree.getList());
		Node node10 = new Node(1, 0, this.tree.getG(), this.tree.getP(), NodeType.Type.VIRTUALNODE, rootHandler,
				NodeType.Position.LEFT);
		this.tree.addNewNode(node10);
		Node node11 = new Node(1, 1, this.tree.getG(), this.tree.getP(), NodeType.Type.VIRTUALNODE, rootHandler,
				NodeType.Position.RIGHT);
		this.tree.addNewNode(node11);
		rootHandler.left = node10;
		rootHandler.right = node11;
	}

	public static void main(String args[]) {

		server = new ChatServer();

	}

}

class clientThread extends Thread {

	private String clientName = null;
	private BufferedReader is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxClientsCount;
	private int user_number;

	public clientThread(Socket clientSocket, clientThread[] threads, int user_number) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
		this.user_number = user_number;
	}

	public void run() {
		int maxClientsCount = this.maxClientsCount;
		clientThread[] threads = this.threads;

		try {
			is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			os = new PrintStream(clientSocket.getOutputStream());
			String name = "User" + this.user_number;
			while (true) {
				// os.println("Enter your name.");
				// name = is.readLine().trim();
				os.println("Your name for tests is User" + this.user_number);
				name = "User" + this.user_number;
				if (name.indexOf('@') == -1) {
					break;
				} else {
					os.println("The name should not contain '@' character.");
				}
			}

			os.println("Welcome " + name + " to our chat room.\nTo leave enter /quit in a new line.");
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = "@" + name;
						break;
					}
				}
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this) {
						threads[i].os.println("*** A new user " + name + " entered the chat room !!! ***");
					}
				}
			}

			while (true) {
				String line = is.readLine();
				if (line.startsWith("/quit")) {
					break;
				}
				/* If the message is private sent it to the given client. */
				if (line.startsWith("@")) {
					String[] words = line.split("\\s", 2);
					if (words.length > 1 && words[1] != null) {
						words[1] = words[1].trim();
						if (!words[1].isEmpty()) {
							synchronized (this) {
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null && threads[i] != this && threads[i].clientName != null
											&& threads[i].clientName.equals(words[0])) {
										threads[i].os.println("<" + name + ">:" + words[1]);
										/*
										 * Echo this message to let the client
										 * know the private message was sent.
										 */
										this.os.println(">" + name + ">:" + words[1]);
										break;
									}
								}
							}
						}
					}
				} else {
					/*
					 * The message is public, broadcast it to all other clients.
					 */
					synchronized (this) {
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null && threads[i].clientName != null) {
								threads[i].os.println("<" + name + ">:" + line);
							}
						}
					}
				}
			}
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].clientName != null) {
						threads[i].os.println("*** The user " + name + " is leaving the chat room !!! ***");
					}
				}
			}
			os.println("*** Bye " + name + " ***");

			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}

			is.close();
			os.close();
			clientSocket.close();
		} catch (IOException e) {
			
		}
	}
}