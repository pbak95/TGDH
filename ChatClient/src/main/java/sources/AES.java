package sources;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class AES {
	public static byte[] encrypt(SecretKey key, byte[] value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value);
            return encrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static byte[] decrypt(SecretKey key, byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] original = cipher.doFinal(encrypted);
            return original;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
//    	SecretKey key = null;
//    	String test = "Hello World";
//		try {
//			key = KeyGenerator.getInstance("AES").generateKey();
//			System.out.println("wiadomosc: "+test);
//			System.out.println("dlogosc klucza: "+key.getEncoded().length);
//			byte[] temp = encrypt(key, test.getBytes());
//	        System.out.println("encrypted string: " + new String(temp));
//			byte[] temp2 = decrypt(key, temp);
//            System.out.println("decrypted string: " + new String(temp2));
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
    }
}
