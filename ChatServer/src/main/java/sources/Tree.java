package sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import sources.Node;
import java.math.BigInteger;
//server version
public class Tree{
	
	private BigInteger g;
	
	private BigInteger p;
	
	private Node root;

	private ArrayList<Node> list;
	
	public int height;
	//private DiffieHellman diffieHellman;
	public ClientService clientServiceHandler;

	private void initParams(){
		//Random rnd = new Random();
		//this.g = BigInteger.probablePrime(128, rnd);
		//Random rnd2 = new Random();
		//this.p = BigInteger.probablePrime(128, rnd2);
		this.p = new BigInteger("B10B8F96A080E01DDE92DE5EAE5D54EC52C99FBCFB06A3C6"
				+ "9A6A9DCA52D23B616073E28675A23D189838EF1E2EE652C0"
				+ "13ECB4AEA906112324975C3CD49B83BFACCBDD7D90C4BD70"
				+ "98488E9C219A73724EFFD6FAE5644738FAA31A4FF55BCCC0"
				+ "A151AF5F0DC8B4BD45BF37DF365C1A65E68CFDA76D4DA708"
				+ "DF1FB2BC2E4A4371",16);
		this.g = new BigInteger("A4D1CBD5C3FD34126765A442EFB99905F8104DD258AC507F"
				+ "D6406CFF14266D31266FEA1E5C41564B777E690F5504F213"
				+ "160217B4B01B886A5E91547F9E2749F4D7FBD7D3B9A92EE1"
				+ "909D0D2263F80A76A6A24C087A091F531DBF0A0169B6A28A"
				+ "D662A4D18E73AFA32D779D5918D08BC8858F4DCEF97C2A24"
				+ "855E6EEB22B3B2E5",16);
	}
	
	private void initTree(){
		
		Node root = new Node(0,0,g,p,NodeType.Type.VIRTUALNODE, null, null);
		list = new ArrayList<Node>();
		list.add(root);
	}
	
	public ArrayList<Node> getList() {
		return list;
	}
	
	public void addNewNode(Node new_node){
		this.list.add(new_node);
	}

	public Tree() {
	list = new ArrayList<Node>();
	//this.diffieHellman = new DiffieHellman();
	this.initParams();
	this.initTree();
	}
	
	public BigInteger getG() {
		return g;
	}

	public BigInteger getP() {
		return p;
	}

	public Node getRoot() {
		return this.root;
	}
	public Node getRight(Node node) {
		return node.right;
	}
	public Node getLeft(Node node) {
		return node.left;
	}
	public void setBK(int l, int v, byte[] BK) {
		Node node = this.getByPosition(l,v,this.list);
		node.setBlindKey(BK);
	}
	public byte[] getBK(int l, int v) {
		Node node = this.getByPosition(l,v,this.list);
		return node.getBlindKey();
	}
	public Node getByPosition(int l,int v, List<Node> list) {
		for(int i=0;i<list.size();i++) {
			int[]temp = list.get(i).getposition();
			if(temp[0] == l && temp[1] == v)
			{
				return list.get(i);
			}
		}
		return null;
	}
	
	
	static class NodeType {
	    public enum Type {
	        LEAFNODE, VIRTUALNODE;
	    }
	    public enum Position{
	    	LEFT, RIGHT;
	    }
	}	
	
}

