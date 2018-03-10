package sec;

import java.io.*;
import java.security.SecureRandom;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;

public abstract class CipherHelper {
	
	public static final SecureRandom RANDOM = new SecureRandom();
	public static final byte AES_MAGIC = 0;
	public static final byte DES_MAGIC = 1;
	public static final byte BF_MAGIC = 2;
	public static final DESHelper DES = new DESHelper();
	public static final AESHelper AES = new AESHelper();
	public static final BlowfishHelper BF = new BlowfishHelper();
	public static final int PAD = 23;
	
	public static CipherHelper getHelper (byte magic) {
		switch (magic) {
			case AES_MAGIC:
				return AES;
			case DES_MAGIC:
				return DES;
			case BF_MAGIC:
				return BF;
			default:
				throw new RuntimeException("unknown magic " + magic);
		}
	}
	
	public static SecretKey getPasswordKey (char[] pass, byte[] salt, int iterations, int length) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		PBEKeySpec keySpec = new PBEKeySpec(pass, salt, iterations, length);
		long t = System.nanoTime();
		SecretKey key = factory.generateSecret(keySpec);
		t = System.nanoTime() - t;
		System.out.println("password key iteration time " + (t / 1000000000.0));
		return key;
	}

	public static void pad (final OutputStream os) throws IOException {
		byte[] a = new byte[PAD];
		CipherHelper.RANDOM.nextBytes(a);
		for (int n = 0; n < a.length; n++) {
			os.write(a[n]);
		}
	}
	
	public static void unpad (final InputStream is) throws IOException {
		for (int n = 0; n < PAD; n++) {
			is.read();
		}
	}
	
	public abstract byte getMagic ();
	
	public abstract SecretKey getKey (char[] pass) throws Exception;
	
	public abstract Cipher getEncryptCipher (OutputStream os, SecretKey key) throws Exception;
	
	public abstract Cipher getDecryptCipher (InputStream is, SecretKey key) throws Exception;
}
