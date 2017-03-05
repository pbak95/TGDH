package sources;

import java.io.Serializable;

public class Packet implements Serializable,Cloneable {
	
	public int l;
	public int v;
	public byte[] blindkey;
	
	public Packet(int l, int v, byte[] blindkey) {
		this.l = l;
		this.v = v;
		this.blindkey = blindkey;
	}
	
	protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
	
	
}
