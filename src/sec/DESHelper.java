package sec;

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.*;
import javax.crypto.spec.*;

public class DESHelper extends CipherHelper {

	public static final String DES_ALGO = "DESede";
	public static final String DES_TRANS = "DESede/CBC/PKCS5Padding";
	public static final byte[] DES_SALT = new byte[] { -127, -49, 112, 51, 93, -57, -80, 78 };
	public static final int DES_ITERATIONS = 575258;
	public static final int DES_KEY_LENGTH = 192;
	
	@Override
	public byte getMagic () {
		return DES_MAGIC;
	}
	
	@Override
	public SecretKey getKey (char[] pass) throws Exception {
		SecretKey key = getPasswordKey(pass, DES_SALT, DES_ITERATIONS, DES_KEY_LENGTH);
		SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance(DES_ALGO);
		DESedeKeySpec desKeySpec = new DESedeKeySpec(key.getEncoded());
		return desKeyFactory.generateSecret(desKeySpec);
	}

	@Override
	public Cipher getEncryptCipher (OutputStream os, SecretKey key) throws Exception {
		byte[] iv = new byte[8];
		RANDOM.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(DES_TRANS);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
		os.write(iv);
		return cipher;
	}
	
	@Override
	public Cipher getDecryptCipher (InputStream is, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(DES_TRANS);
		final byte[] iv = new byte[8];
		for (int n = 0; n < iv.length; n++) {
			iv[n] = (byte) is.read();
		}
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
		return cipher;
	}
	
	@Override
	public String toString () {
		return DES_ALGO;
	}
}
