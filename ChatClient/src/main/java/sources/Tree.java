package sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.math.BigInteger;
import sources.Node;
//client version
public class Tree {

	private BigInteger g;

	private BigInteger p;

	private Node root;

	private ArrayList<Node> list;

	public int height;
	
	public int my_l;
	
	public int my_v;
	
	public NodeType.Position my_position;
	// private DiffieHellman diffieHellman;

	private void initParams() {
		Random rnd = new Random();
		this.g = BigInteger.probablePrime(1024, rnd);
		Random rnd2 = new Random();
		this.p = BigInteger.probablePrime(1024, rnd2);
	}

	private void initTree() {

		this.root = new Node(0, 0, g, p, NodeType.Type.VIRTUALNODE, null, null);
		list = new ArrayList<Node>();
		list.add(root);
		initializeTreeFor4Users();
	}

	public ArrayList<Node> getList() {
		return list;
	}

	public void addNewNode(Node new_node) {
		this.list.add(new_node);
	}

	public Tree(BigInteger g, BigInteger p) {

		this.g = g;
		this.p = p;
		this.initTree();
	}
	
	public void currentPosition(int l, int v){
		Node current = this.getByPosition(l, v, this.getList());
		current.constructUserNode(current);
		if((v % 2) == 0){
			this.my_position = NodeType.Position.LEFT;
		}else if ((v % 2) != 0){
			this.my_position = NodeType.Position.RIGHT;
		}
		this.my_l = l;
		this.my_v = v;
		
		System.out.println("BK for <"+my_l+","+my_v+"> : "+Arrays.toString(current.getBlindKey()));
	}

	public BigInteger getG() {
		return this.g;
	}

	public BigInteger getP() {
		return this.p;
	}

	public void initializeTreeFor4Users() {

		Node rootHandler = this.getByPosition(0, 0, this.getList());
		Node node10 = new Node(1, 0, this.getG(), this.getP(), NodeType.Type.VIRTUALNODE, rootHandler,
				NodeType.Position.LEFT);
		this.list.add(node10);
		Node node11 = new Node(1, 1, this.getG(), this.getP(), NodeType.Type.VIRTUALNODE, rootHandler,
				NodeType.Position.RIGHT);
		this.list.add(node11);
		Node node20 = new Node(2, 0, this.getG(), this.getP(), NodeType.Type.LEAFNODE, node10, NodeType.Position.LEFT);
		this.list.add(node20);
		Node node21 = new Node(2, 1, this.getG(), this.getP(), NodeType.Type.LEAFNODE, node10, NodeType.Position.RIGHT);
		this.list.add(node21);
		Node node22 = new Node(2, 2, this.getG(), this.getP(), NodeType.Type.LEAFNODE, node11, NodeType.Position.LEFT);
		this.list.add(node22);
		Node node23 = new Node(2, 3, this.getG(), this.getP(), NodeType.Type.LEAFNODE, node11, NodeType.Position.RIGHT);
		this.list.add(node23);
		this.height = 2;

		rootHandler.left = node10;
		rootHandler.right = node11;
		rootHandler.left.left = node20;
		rootHandler.left.right = node21;
		rootHandler.right.left = node22;
		rootHandler.right.right = node23;

		
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
		Node node = this.getByPosition(l, v, this.list);
		node.setBlindKey(BK);
	}

	public byte[] getBK(int l, int v) {
		Node node = this.getByPosition(l, v, this.list);
		return node.getBlindKey();
	}

	public Node getByPosition(int l, int v, List<Node> list) {
		for (int i = 0; i < list.size(); i++) {
			int[] temp = list.get(i).getposition();
			if (temp[0] == l && temp[1] == v) {
				return list.get(i);
			}
		}
		return null;
	}

	static class NodeType {
		public enum Type {
			LEAFNODE, VIRTUALNODE;
		}

		public enum Position {
			LEFT, RIGHT;
		}
	}
}




