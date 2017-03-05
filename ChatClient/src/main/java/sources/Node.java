package sources;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import sources.Tree.NodeType;

public class Node {
	
	private int l,v; //position in tree
	private BigInteger g, p; //big integers
	private byte[] K = null; //secret key
	public byte[] BK = null; //blind public key
	public byte[] GK = null; //calculated group key
	private NodeType.Type type;
	private NodeType.Position pos;
	private String username = null; //name of user
	public Node parent= null;
	Node left = null;
	Node right = null;
	
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
	
	//leafnode
	public Node(int l, int v, BigInteger g,BigInteger p, String username, NodeType.Type type, Node parent,NodeType.Position pos) {
		this.l = l;
		this.v = v;
		this.g = g;
		this.p = p;
		this.K = generateKey();//64 bajty klucza
		this.BK = BKcalculation(g, p, this.getK());//wynik 128 bajtow dla g,p 1024 bitow
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
	
	public byte[] BKcalculation(BigInteger g, BigInteger p, byte[] K) {
		BigInteger k = new BigInteger(K);
		BigInteger temp = g.modPow(k, p);
		return temp.toByteArray();
	}
	
	public void Kcalculation(byte[] bkey, byte[] key) {
		BigInteger k = new BigInteger(key);
		BigInteger bk = new BigInteger(bkey);
		BigInteger temp = bk.modPow(k, p);
		this.K = temp.toByteArray();
		this.BK = BKcalculation(this.g,this.p, this.K);
	}
	
	public void groupKCalculation(int height){
		
		Node tmp_node = this;
		BigInteger tmp_key = new BigInteger(tmp_node.getK());
		for(int i=0;i<height;i++) {	
			if(tmp_node.pos == NodeType.Position.RIGHT) {
				tmp_key= new BigInteger(tmp_node.parent.left.getBlindKey()).modPow(tmp_key, this.p);
			}else{
				tmp_key= new BigInteger(tmp_node.parent.right.getBlindKey()).modPow(tmp_key, this.p);
			}
				tmp_node = tmp_node.parent;
		}
		
		this.GK = tmp_key.toByteArray();
		
	}
	
	public void showKey(){
		System.out.println("Node<"+l+","+v+"> KEY: "+ this.getGK().toString());
	}
	
	public byte[] getK() {
		return K;
	}
	
	public void setK(byte[] k) {
		this.K = k;
	}

	public byte[] getGK() {
		return this.GK;
	}

	public void setBlindKey(byte[] bk){
		this.BK = bk;
	}
	
	public byte[] setParentKey(){
		BigInteger temp;
		if(this.pos == NodeType.Position.RIGHT) {
			temp = new BigInteger(this.parent.left.getBlindKey()).modPow(new BigInteger(this.getK()), p);
			byte[] key = temp.toByteArray();
			return key;
		}else{
			temp = new BigInteger(this.parent.right.getBlindKey()).modPow(new BigInteger(this.getK()), p);
			byte[] key = temp.toByteArray();
			return key;
		}
	}

	public byte[] getBlindKey() {
		//if(this.type == NodeType.Type.LEAFNODE) {
			return this.BK;
		//}
		//else {
			//BigInteger temp = new BigInteger(this.left.getBlindKey()).modPow(new BigInteger(this.right.setParentKey()), p);
			//byte[] key = temp.toByteArray();
			//return this.BKcalculation(this.g, this.p, key);
		//}
	}
	public int[] getposition() {
		int[] position = new int[2];
		position[0] = this.l;
		position[1] = this.v;
		return position;
	}
	private byte[] generateKey() {
		//generate random 16 byte key
		SecretKey secretkey;
		byte[] key = null;
		try {
			secretkey = KeyGenerator.getInstance("AES").generateKey();
			key = secretkey.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		  return key;
	}

	public void constructUserNode(Node current) {
		current.K = generateKey();//64 bajty klucza
		current.BK = BKcalculation(g, p, this.getK());//wynik 128 bajtow dla g,p 1024 bitow
	}
	
}
