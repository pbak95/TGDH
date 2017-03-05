package sources;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;

public class Params implements Serializable{
	
	public BigInteger g;

	public BigInteger p;
	
	public PublicKey publicKey;
	
	public Params(BigInteger g, BigInteger p, PublicKey key) {
		this.g = g;
		this.p = p;
		this.publicKey = key;
	}
	
}
