package sources;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sources.Tree.NodeType;

public class ChatClient implements Runnable {

	// ChatClient reference
	private static ChatClient client;
	// The client socket
	private static Socket clientSocket = null;
	// The output stream
	private static PrintStream os = null;
	// The input stream
	private static BufferedReader is = null;
	/** socket commands */
	private static Socket signalingSocket1 = null;
	/** socket objects */
	private static Socket signalingSocket2 = null;
	/** input stream */
	private static BufferedReader is_s;
	/** output stream */
	private static PrintWriter os_s;
	/** object to send */
	private ObjectOutputStream outputStream;
	/** object to send */
	private ObjectInputStream inputStream;

	private static Scanner inputLine = null;

	// Broadcast port
	private static int b_port_number = 2222;
	// Signaling port for commands
	private static int s_c_port_number = 13389;
	// Signaling port for objects
	private static int s_o_port_number = 13390;
	// host address
	private static String host = "localhost";
	// thread to communication
	private Thread message_t;
	// thread to signaling
	private Thread signaling_t;

	private byte[] groupkey = null;
	public Tree tree;
	private static boolean cipherReady = false;
	private PublicKey publicKeyForSignature;
	private Signature signature;
	private static boolean signatureReady = false;
	
	private int counter = 0;
	private int RefreshKeyAfterMessages = 20;

	public ChatClient() {
		System.out.println("Usage: java MultiThreadChatClient <host> <portNumber>\n" + "Now using host=" + host
				+ ", portNumber=" + b_port_number);

		/*
		 * Open a socket on a given host and port. Open input and output
		 * streams.
		 */
		try {
			clientSocket = new Socket(host, b_port_number);
			signalingSocket1 = new Socket(host, s_c_port_number);
			signalingSocket2 = new Socket(host, s_o_port_number);
			inputLine = new Scanner(System.in);
			os = new PrintStream(clientSocket.getOutputStream());
			is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			// -----------------------
			is_s = new BufferedReader(new InputStreamReader(signalingSocket1.getInputStream()));
			os_s = new PrintWriter(signalingSocket1.getOutputStream(), true);
			outputStream = new ObjectOutputStream(signalingSocket2.getOutputStream());
			inputStream = new ObjectInputStream(signalingSocket2.getInputStream());
			initializeSignature();
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host " + host);
		}
		// this.tree = new Tree(user_number);
	}

	/** flush output */
	public void flushOutput() {
		os_s.flush();
	}
	
	private void initializeSignature(){
		Security.addProvider(new BouncyCastleProvider());
		try{
	    signature = Signature.getInstance("SHA1withRSA", "BC");
		}catch(NoSuchProviderException e){
			e.printStackTrace();
		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}
	}

	/*
	 * Create a thread to read from the server. (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive "Bye" from the
		 * server. Once we received that then we want to break.
		 */

		this.message_t = new Thread(new Runnable() {

			public void run() {
				try {
					String responseLine = is.readLine();
					while ((responseLine = is.readLine()) != null) {

						if (responseLine != null && cipherReady == false) {
							System.out.println(responseLine);
							if (responseLine.indexOf("*** Bye") != -1) {

								client.sendCommand(Protocol.QUIT);
								os.close();
								is.close();
								clientSocket.close();
								signalingSocket1.close();
								signalingSocket2.close();
								System.exit(0);
							}
						} else {
							if (responseLine.indexOf("*** Bye") != -1) {

								client.sendCommand(Protocol.QUIT);
								os.close();
								is.close();
								clientSocket.close();
								signalingSocket1.close();
								signalingSocket2.close();
								System.exit(0);
							}else if(responseLine.startsWith("*** ")){
								System.out.println(responseLine);
							}else{
							
							SecretKey key = new SecretKeySpec(groupkey, "AES");
							String[] parts = responseLine.split(":");
							String part1 = parts[0];
							String part2 = parts[1];
							System.out.println("Received string: " + part2);
							//System.out.println("Bytes after getBytes: " + Arrays.toString(toByteArray(part2)));
							String response = new String(AES.decrypt(key, toByteArray(part2)));
							System.out.println(part1 + ":" + response);
							counter++;
							if(counter == RefreshKeyAfterMessages) {
								counter = 0;
								client.sendCommand(Protocol.TESTCONNECTION);
							}
						}
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		message_t.start();

		this.sendCommand(Protocol.TESTCONNECTION);

		this.signaling_t = new Thread(new Runnable() {
			public void run() {

				while (true)
					try {
						String command = is_s.readLine();
						if (signatureReady == false) {
							if (!handleCommand(command)) {
								os_s.close();
								is_s.close();
								outputStream.close();
								inputStream.close();
								signalingSocket1.close();
								signalingSocket2.close();
								break;
							}
						} else {
							try {
								String[] parts = command.split(" ");
								String commandType = parts[0];
								String commandSign = parts[1];
								if (verifySig(commandType.getBytes(), publicKeyForSignature, toByteArray(commandSign))) {
									System.out.println("server signature veryfied");
									if (!handleCommand(commandType)) {
										os_s.close();
										is_s.close();
										outputStream.close();
										inputStream.close();
										signalingSocket1.close();
										signalingSocket2.close();
										break;
									}
								}else{
									System.out.println("unknown server signature");
									os_s.close();
									is_s.close();
									outputStream.close();
									inputStream.close();
									signalingSocket1.close();
									signalingSocket2.close();
									break;
								}
							} catch (InvalidKeyException e) {
								e.printStackTrace();
							} catch (SignatureException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					} catch (IOException e) {
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				os_s = null;
				is_s = null;
				synchronized (this) {
					signalingSocket1 = null;
					signalingSocket2 = null;
					System.out.println("Connection lost");
				}

			}
		});
		signaling_t.start();

	}

	/**
	 * method which handle signaling commands from server
	 * 
	 * @param command
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	synchronized private boolean handleCommand(String command) throws IOException, NoSuchAlgorithmException {

		StringTokenizer st = new StringTokenizer(command);
		String cd = st.nextToken();
		if (cd.equals(Protocol.CONNECTED)) {
			System.out.println("Server signallization confirmation");
			this.sendCommand(Protocol.READYFORPARAMS);
		} else if (cd.equals(Protocol.SETPARAMS)) {
			Params params;
			try {
				params = (Params) this.inputStream.readObject();
				this.tree = new Tree(params.g, params.p);
				this.publicKeyForSignature = params.publicKey;
				signatureReady = true;
				this.sendCommand(Protocol.READYFORPOSITION);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.SETPOSITION)) {
			Packet packet;
			try {
				packet = (Packet) this.inputStream.readObject();
				System.out.println("My position:<" + packet.l + "," + packet.v + ">");
				this.tree.currentPosition(packet.l, packet.v);
				sendCommand(Protocol.READYFORBKDISTRIBUTION);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.INITIALIZEBKDISTRIBUTION)) {
			sendCommand(Protocol.GETLEAFBK);
			Packet packet = new Packet(this.tree.my_l, this.tree.my_v - 1,
					this.tree.getBK(this.tree.my_l, this.tree.my_v));
			try {
				outputStream.reset();
				outputStream.writeObject(packet);
				outputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.GETBK)) {
			if (this.tree.my_position == NodeType.Position.LEFT) {
				sendCommand(Protocol.MYBKLEFT);
			} else {
				sendCommand(Protocol.MYBKRIGHT);
			}
			Packet packet = new Packet(this.tree.my_l, this.tree.my_v, this.tree.getBK(this.tree.my_l, this.tree.my_v));
			try {
				outputStream.reset();
				outputStream.writeObject(packet);
				outputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.NEIGHBORBK)) {
			//System.out.println("Received NEIGHBORBK jestem <" + this.tree.my_l + "," + this.tree.my_v + ">");
			sendCommand(Protocol.NEIGHBORBKAKC);
			Packet packet;
			try {
				packet = (Packet) this.inputStream.readObject();
				System.out.println("Received BK from: <" + packet.l + "," + packet.v + ">");
				System.out.println("BK from <" + packet.l + "," + packet.v + "> : " + Arrays.toString(packet.blindkey));
				this.tree.setBK(packet.l, packet.v, packet.blindkey);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (this.tree.my_position == NodeType.Position.RIGHT) {
				sendCommand(Protocol.GETLEAFBK);
				Packet packet2 = new Packet(this.tree.my_l, this.tree.my_v,
						this.tree.getBK(this.tree.my_l, this.tree.my_v));
				try {
					outputStream.reset();
					outputStream.writeObject(packet2);
					outputStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				sendCommand(Protocol.BKDISTRIBUATED);
			}
		} else if (cd.equals(Protocol.CALCULATEPARENTBK)) {
			System.out.println("received calculateparentbk");
			Node current = this.tree.getByPosition(this.tree.my_l, this.tree.my_v, this.tree.getList());
			byte[] parentK = current.setParentKey();
			current.parent.setBlindKey(current.BKcalculation(this.tree.getG(), this.tree.getP(), parentK));
			System.out.println("calculated parentbk " + Arrays.toString(current.getBlindKey()));
			this.sendCommand(Protocol.PARENTBKCALCULATED);
		} else if (cd.equals(Protocol.GETPARENT)) {
			System.out.println("received getparent");
			Node parent = this.tree.getByPosition(this.tree.my_l, this.tree.my_v, this.tree.getList()).parent;
			Packet packet = new Packet(parent.getposition()[0], parent.getposition()[1], parent.getBlindKey());
			this.sendCommand(Protocol.SETPARENT);
			try {
				outputStream.reset();
				outputStream.writeObject(packet);
				outputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.OTHERPARENT)) {
			sendCommand(Protocol.PARENTOK);
			Packet packet;
			try {
				packet = (Packet) this.inputStream.readObject();
				System.out.println("Received BK from: <" + packet.l + "," + packet.v + ">");
				System.out.println("BK from <" + packet.l + "," + packet.v + "> : " + Arrays.toString(packet.blindkey));
				this.tree.setBK(packet.l, packet.v, packet.blindkey);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (cd.equals(Protocol.GKOK)) {
			System.out.println("GK calculation");
			this.tree.getByPosition(this.tree.my_l, this.tree.my_v, this.tree.getList()).groupKCalculation(2);
			byte[] gk = this.tree.getByPosition(this.tree.my_l, this.tree.my_v, this.tree.getList()).getGK();
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(gk);
			this.groupkey = thedigest;

			System.out.println("GK: " + Arrays.toString(this.groupkey));
			cipherReady = true;
		} else {

			this.sendCommand(Protocol.QUIT);
			return false;
		}
		return true;

	}

	/**
	 * send command method
	 * 
	 * @param command
	 */
	synchronized void sendCommand(String command) {
		if (os_s != null)
			os_s.println(command);
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}
	
	public boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
		signature = Signature.getInstance("SHA1withRSA", "BC");
		signature.initVerify(key);
		signature.update(data);
		return (signature.verify(sig));

	}

	public static void main(String[] args) {

		client = new ChatClient();

		String message;
		new Thread(client).start();
		while (true) {

			if (inputLine.hasNextLine()) {
				if (cipherReady == true) {
					message = inputLine.nextLine();
					if (message.startsWith("@")) {
						String[] words = message.split("\\s", 2);
						SecretKey key = new SecretKeySpec(client.groupkey, "AES");
						byte[] toSend = AES.encrypt(key, words[1].getBytes());
						System.out.println("Encrypted message in bytes: " + Arrays.toString(toSend));
						System.out.println("sended string : " + words[0] + toHexString(toSend));
						os.println(words[0] + " " + toHexString(toSend));

					}else if(message.startsWith("/quit")){
						os.println(message);
					}else {
					
						SecretKey key = new SecretKeySpec(client.groupkey, "AES");
						byte[] toSend = AES.encrypt(key, message.getBytes());
						System.out.println("Encrypted message in bytes: " + Arrays.toString(toSend));
						System.out.println("sended string toString: " + toHexString(toSend));
						os.println(toHexString(toSend));

					}
				}
				
				
			}

		}

	}

}