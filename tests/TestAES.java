import org.junit.Test;
import sec.AESHelper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sec.CipherHelper.getPasswordKey;

public class TestAES {
    public static final String AES_TRANS = "AES/CBC/PKCS5Padding";
    public static final String AES_ALGO = "AES";
    public static final byte[] AES_SALT = new byte[]{-70, 93, -16, 40, -72, -27, -109, -78};
    public static final int AES_ITERATIONS = 704272;
    public static final int AES_KEY_LENGTH = 128;

    @Test
    public void testGetKeyvalidinput() throws Exception {
        char[] pass = {'n', 'i', 'k', 'h', 'i', 't', 'a'};
        final SecretKey key = getPasswordKey( pass, AES_SALT, AES_ITERATIONS, AES_KEY_LENGTH );
        SecretKey expectedKey = new SecretKeySpec( key.getEncoded(), AES_ALGO );
        AESHelper aeshelper = new AESHelper();
        assertEquals( expectedKey, aeshelper.getKey( pass ) );
        assertNotNull( aeshelper.getKey( pass ) );
    }

    @Test
    public void testgetEncryptcipher()throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANS);
        // creating a fake output stream
        String output_File = "C:/Users/nivesh/TestFile.txt";
        FileOutputStream fos = new FileOutputStream(output_File);
        //creating a new secret key
        String str = "nikhita";
        char[] charArray = str.toCharArray();
        AESHelper mockAeshelper = new AESHelper();
        SecretKey key = mockAeshelper.getKey(charArray);
        System.out.println(mockAeshelper.getEncryptCipher(fos,key));
        assertNotNull(mockAeshelper.getEncryptCipher(fos,key));
    }
   @Test
    public void testgetDecryptcipher()throws Exception{
       //creating a fake input stream
       String Input_File = "C:/Users/nivesh/TestFile.txt";
       FileInputStream fis = new FileInputStream(Input_File);
       //creating a secret key
       String str = "nikhita";
       char[] charArray = str.toCharArray();
       AESHelper mockAeshelper = new AESHelper();
       SecretKey key = mockAeshelper.getKey(charArray);
       System.out.println( mockAeshelper.getDecryptCipher(fis,key));
       assertNotNull( mockAeshelper.getDecryptCipher(fis,key) );
    }
}