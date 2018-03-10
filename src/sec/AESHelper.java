package sec;

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.*;
import javax.crypto.spec.*;

public class AESHelper extends CipherHelper {
	
	public static final String AES_TRANS = "AES/CBC/PKCS5Padding";
	public static final String AES_ALGO = "AES";
	public static final byte[] AES_SALT = new byte[] { -70, 93, -16, 40, -72, -27, -109, -78 };
	public static final int AES_ITERATIONS = 704272;
	public static final int AES_KEY_LENGTH = 128;
	
	@Override
	public byte getMagic () {
		return AES_MAGIC;
	}
	
	@Override
	public SecretKey getKey (char[] pass) throws Exception {
		final SecretKey key = getPasswordKey(pass, AES_SALT, AES_ITERATIONS, AES_KEY_LENGTH);
		return new SecretKeySpec(key.getEncoded(), AES_ALGO);
	}
	
	@Override
	public Cipher getEncryptCipher (OutputStream os, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(AES_TRANS);
		byte[] iv = new byte[16];
		RANDOM.nextBytes(iv);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
		os.write(iv);
		return cipher;
	}
	
	@Override
	public Cipher getDecryptCipher (InputStream is, SecretKey key) throws Exception {
		final Cipher cipher = Cipher.getInstance(AES_TRANS);
		final byte[] iv = new byte[16];
		for (int n = 0; n < iv.length; n++) {
			iv[n] = (byte) is.read();
		}
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
		return cipher;
	}
	
	@Override
	public String toString () {
		return AES_ALGO;
	}
}
