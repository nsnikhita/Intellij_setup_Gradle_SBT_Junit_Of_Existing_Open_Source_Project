package sec;

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BlowfishHelper extends CipherHelper {
	
	private static final String BF_ALGO = "Blowfish";
	private static final int BF_ITER = 738892;
	private static final int BF_KEYLEN = 128;
	private static final byte[] BF_SALT = new byte[] { 60, -39, 41, -99, -126, 9, 54, -100 };
	private static final String BF_TRANS = "Blowfish/CBC/PKCS5Padding";
	
	@Override
	public byte getMagic () {
		return BF_MAGIC;
	}

	@Override
	public SecretKey getKey (char[] pass) throws Exception {
		SecretKey key = CipherHelper.getPasswordKey("password".toCharArray(), BF_SALT, BF_ITER, BF_KEYLEN);
		return new SecretKeySpec(key.getEncoded(), BF_ALGO);
	}

	@Override
	public Cipher getEncryptCipher (OutputStream os, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(BF_TRANS);
		byte[] iv = new byte[8];
		RANDOM.nextBytes(iv);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
		os.write(iv);
		return cipher;
	}

	@Override
	public Cipher getDecryptCipher (InputStream is, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(BF_TRANS);
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
		return BF_ALGO;
	}
}
