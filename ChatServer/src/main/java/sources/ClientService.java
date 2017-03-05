package sources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.StringTokenizer;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sources.Tree.NodeType;

public class ClientService implements Runnable {

	private int id;

	private ChatServer server;
	// commands
	private Socket clientSocket1;
	// objects
	private Socket clientSocket2;

	private ObjectInputStream inStream;

	private BufferedReader input;

	private PrintWriter output;
	/** object to send */
	public ObjectOutputStream outStream;

	private int l;

	private int v;

	public static Packet packet_to_send;

	public static Node destinationNode;

	private static boolean signing = false;

	private KeyPair keyPair;

	private Signature signature;

	public ClientService(Socket clientSocket1, Socket clientSocket2, ChatServer server, int id) {
		this.server = server;
		this.clientSocket1 = clientSocket1;
		this.clientSocket2 = clientSocket2;
		this.id = id;
	}

	void init() throws IOException {
		Reader reader = new InputStreamReader(clientSocket1.getInputStream());
		input = new BufferedReader(reader);
		output = new PrintWriter(clientSocket1.getOutputStream(), true);
		initializeSignature();

	}

	void close() {
		try {
			output.close();
			input.close();
			clientSocket1.close();
			clientSocket2.close();

		} catch (IOException e) {
			//System.err.println("Error closing client (" + id + ").");
			System.out.println("Conection lost with client " + id);
		} finally {
			output = null;
			input = null;
			clientSocket1 = null;
			clientSocket2 = null;
			inStream = null;
		}
	}

	public void run() {
		try {
			inStream = new ObjectInputStream(clientSocket2.getInputStream());
			outStream = new ObjectOutputStream(clientSocket2.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			String request = receive();
			StringTokenizer st = new StringTokenizer(request);
			String command = st.nextToken();

			if (command.equals(Protocol.TESTCONNECTION)) {
				//System.out.println("cos tam przyszlo siecia sygnalizacyjna wow");
				this.send(Protocol.CONNECTED);
			} else if (command.equals(Protocol.READYFORPARAMS)) {
				this.send(Protocol.SETPARAMS);
				Params params = new Params(this.server.g, this.server.p, this.keyPair.getPublic());
				try {
					outStream.reset();
					outStream.writeObject(params);
					outStream.flush();
					

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// this.send(Protocol.SETPOSITION);

			} else if (command.equals(Protocol.READYFORPOSITION)) {
				signing = true;
				this.send(Protocol.SETPOSITION);
				Packet packet = new Packet(this.l, this.v, null);
				try {
					outStream.reset();
					outStream.writeObject(packet);
					outStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (command.equals(Protocol.READYFORBKDISTRIBUTION)) {

				this.server.readyleafs++;
				this.server.checkLeafs();

			} else if (command.equals(Protocol.GETLEAFBK)) {

				Packet packet;
				try {
					packet = (Packet) this.inStream.readObject();
					System.out.println("Received bk request for <" + packet.l + "," + packet.v + ">");
					this.server.tree.getByPosition(packet.l, packet.v, this.server.tree.getList()).clientServiceHandler
							.send(Protocol.GETBK);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (command.equals(Protocol.MYBKLEFT)) {
				Packet packet;
				try {
					packet = (Packet) this.inStream.readObject();
					System.out.println("Received bk from <" + packet.l + "," + packet.v + ">");
					packet_to_send = null;
					packet_to_send = (Packet) packet.clone();
					Node BK_node = this.server.tree.getByPosition(packet.l, packet.v, this.server.tree.getList());
					destinationNode = this.server.tree.getByPosition(BK_node.parent.right.getL(),
							BK_node.parent.right.getV(), this.server.tree.getList());
					destinationNode.clientServiceHandler.packet_to_send = (Packet) packet_to_send.clone();
					destinationNode.clientServiceHandler.send(Protocol.NEIGHBORBK);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			} else if (command.equals(Protocol.MYBKRIGHT)) {
				Packet packet;
				try {
					packet = (Packet) this.inStream.readObject();
					System.out.println("Received bk from <" + packet.l + "," + packet.v + ">");
					packet_to_send = null;
					packet_to_send = (Packet) packet.clone();
					Node BK_node = this.server.tree.getByPosition(packet.l, packet.v, this.server.tree.getList());
					destinationNode = this.server.tree.getByPosition(BK_node.parent.left.getL(),
							BK_node.parent.left.getV(), this.server.tree.getList());
					destinationNode.clientServiceHandler.packet_to_send = (Packet) packet_to_send.clone();
					destinationNode.clientServiceHandler.send(Protocol.NEIGHBORBK);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (command.equals(Protocol.NEIGHBORBKAKC)) {
				System.out.println("Send  bk to <" + this.l + "," + this.v + ">");
				try {

					outStream.reset();
					outStream.writeObject(packet_to_send);
					outStream.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (command.equals(Protocol.BKDISTRIBUATED)) {

				this.server.calculatedBranches++;
				if (this.server.calculatedBranches == 2) {
					System.out.println("bk distributed");
					this.server.calculatedBranches = 0;
					System.out.println("send calculateParentBK to all");
					for (int i = 0; i < 4; i++) {
						this.server.tree.getByPosition(2, i, this.server.tree.getList()).clientServiceHandler
								.send(Protocol.CALCULATEPARENTBK);
					}
				}
			} else if (command.equals(Protocol.PARENTBKCALCULATED)) {

				this.server.readyleafs++;
				if (this.server.readyleafs == 4) {
					System.out.println("all parent BK calculated");
					this.server.readyleafs = 0;
					this.server.tree.getByPosition(2, 0, this.server.tree.getList()).clientServiceHandler
							.send(Protocol.GETPARENT);
				}
			} else if (command.equals(Protocol.SETPARENT)) {
				try {
					Packet packet = (Packet) this.inStream.readObject();
					System.out.println("Received bk from <" + packet.l + "," + packet.v + ">");
					packet_to_send = null;
					packet_to_send = (Packet) packet.clone();
					if (this.server.tree.getByPosition(packet.l, packet.v, this.server.tree.getList())
							.getPos() == NodeType.Position.LEFT)
						this.server.parentDistributionRight();
					else
						this.server.parentDistributionLeft();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (command.equals(Protocol.PARENTOK)) {
				System.out.println("Send  bk to <" + this.l + "," + this.v + ">");
				try {

					outStream.reset();
					outStream.writeObject(packet_to_send);
					outStream.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
				this.server.readyleafs++;
				if (this.server.readyleafs == 2) {
					
					this.server.tree.getByPosition(2, 2, this.server.tree.getList()).clientServiceHandler
							.send(Protocol.GETPARENT);
				} else if (this.server.readyleafs == 4) {
					this.server.readyleafs = 0;
				
					System.out.println("sending GK OK to users");
					for (int i = 0; i < 4; i++) {
						this.server.tree.getByPosition(2, i, this.server.tree.getList()).clientServiceHandler
								.send(Protocol.GKOK);
					}
				}
			} else if (command.equals(Protocol.QUIT)) {
				try {
					inStream.close();
					outStream.close();
					input.close();
					output.close();
					server.removeClientService(this);
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
				// server.removeClientService(this);
			} else if (command.equals(Protocol.NULLCOMMAND)) {
				server.removeClientService(this);
				break;
			}
		}

	}

	void send(String command) {
		if (signing == false) {
			output.println(command);
		} else {
			byte[] commandBytes = command.getBytes();
			byte[] sign = {0};
			try {
				sign = signData(commandBytes, this.keyPair.getPrivate());
			} catch (Exception e) {
				e.printStackTrace();
			}
			output.println(command + " " + toHexString(sign));
			
		}

	}

	private String receive() {
		try {
			return input.readLine();
		} catch (IOException e) {
			System.err.println("Error reading client (" + id + ").");
		}
		return Protocol.NULLCOMMAND;
	}

	public int getL() {
		return l;
	}

	public void setL(int l) {
		this.l = l;
	}

	public int getV() {
		return v;
	}

	public void setV(int v) {
		this.v = v;
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}

	private void initializeSignature() {
		Security.addProvider(new BouncyCastleProvider());
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
			keyGen.initialize(512, new SecureRandom());
			keyPair = keyGen.generateKeyPair();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public byte[] signData(byte[] data, PrivateKey key) throws Exception {
		signature = Signature.getInstance("SHA1withRSA", "BC");
		signature.initSign(keyPair.getPrivate(), new SecureRandom());
		signature.update(data);
		return (signature.sign());
	}

	public boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
		signature = Signature.getInstance("SHA1withRSA", "BC");
		signature.initVerify(key);
		signature.update(data);
		return (signature.verify(sig));

	}

}
