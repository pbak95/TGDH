package sources;

import java.math.BigInteger;

import sources.Tree.NodeType;

public class Node {
	
	private int l,v; //position in tree
	private BigInteger g, p; //big integers
	public byte[] BK = null; //blind public key
	private NodeType.Type type;
	private NodeType.Position pos;
	private String username = null; //name of user
	public Node parent= null;
	Node left = null;
	Node right = null;

	public ClientService clientServiceHandler;
	
	//normal node
	public Node(int l, int v, BigInteger g,BigInteger p, NodeType.Type type, Node parent,NodeType.Position pos ) {
		this.l = l;
		this.v = v;
		this.g = g;
		this.p = p;
		this.type =type;
		this.parent = parent;
		this.pos = pos;
	}
	
	//leafnode on server side
		public Node(ClientService clientServiceHandler,int l, int v, BigInteger g,BigInteger p, String username, NodeType.Type type, Node parent,NodeType.Position pos) {
			this.clientServiceHandler = clientServiceHandler;
			this.l = l;
			this.v = v;
			this.g = g;
			this.p = p;
			this.username = username;
			this.type = type;
			this.parent = parent;
			this.pos = pos;
		}

	public NodeType.Type getType() {
		return type;
	}
	
	public NodeType.Position getPos() {
		return pos;
	}

	public void setBlindKey(byte[] bk){
		this.BK = bk;
	}
	
	public byte[] getBlindKey() {
			return this.BK;
	}
	
	public int[] getposition() {
		int[] position = new int[2];
		position[0] = this.l;
		position[1] = this.v;
		return position;
	}

	public int getL() {
		return l;
	}

	public int getV() {
		return v;
	}

	

}
