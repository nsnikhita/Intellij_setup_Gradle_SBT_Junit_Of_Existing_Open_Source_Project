package sec;

import java.security.SecureRandom;
import java.util.Arrays;

public class salt {
	
	public static void main (String[] args) {
		SecureRandom r = new SecureRandom();
		byte[] s = new byte[8];
		r.nextBytes(s);
		System.out.println(Arrays.toString(s));
		System.out.println(500000 + r.nextInt(500000));
	}
	
}
