package net.dijkema.jndbm.util;

/**
* This is a very fast, non-cryptographic hash suitable for general hash-based
* lookup. See http://murmurhash.googlepages.com/ for more details.
*
* <p>The C version of MurmurHash 2.0 found at that site was ported
* to Java by Andrzej Bialecki (ab at getopt org).</p>
*/


public class MurmurHash {
	
	static public int hash(String data,int table_length_prime) {
		byte[] b;
		try {
			b=data.getBytes("UTF-8");
		} catch (Exception E) {
			b=data.getBytes();
		}
		int h=ihash(b,b.length,0);
		if (h<0) { h=h*-1; }
		return h%table_length_prime;
	}
	
	static public int getPrimeLength(int tableLength) {
		int i,N;
		i=0;N=-1;
		int bnum=tableLength;
		while (i!=N) {
			for(i=2,N=bnum/2+1;i<N && bnum%i!=0;i++);
			if (i!=N) { bnum+=1; }
		}
		return bnum;  // Now it is a prime number
	}
	
	static private int ihash(byte[] data, int length, int seed) {
		int m = 0x5bd1e995;
		int r = 24;
 
		int h = seed ^ length;
 
		int len_4 = length >> 2;
 
	    for (int i = 0; i < len_4; i++) {
	      int i_4 = i << 2;
	      int k = data[i_4 + 3];
	      k = k << 8;
	      k = k | (data[i_4 + 2] & 0xff);
	      k = k << 8;
	      k = k | (data[i_4 + 1] & 0xff);
	      k = k << 8;
	      k = k | (data[i_4 + 0] & 0xff);
	      k *= m;
	      k ^= k >>> r;
	      k *= m;
	      h *= m;
	      h ^= k;
	    }
	 
	    // avoid calculating modulo
	    int len_m = len_4 << 2;
	    int left = length - len_m;
	 
	    if (left != 0) {
	      if (left >= 3) {
	        h ^= (int) data[length - 3] << 16;
	      }
	      if (left >= 2) {
	        h ^= (int) data[length - 2] << 8;
	      }
	      if (left >= 1) {
	        h ^= (int) data[length - 1];
	      }
	 
	      h *= m;
	    }
	 
	    h ^= h >>> 13;
	    h *= m;
	    h ^= h >>> 15;
	 
	    return h;
	  }
}