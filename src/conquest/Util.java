package conquest;

import java.util.*;

public class Util {
	
	public static final Random RANDOM = new Random();
	
	public static int log2OfPowerOf2(long num) {
		
		int log2 = (num & 0xAAAAAAAAAAAAAAAAL) != 0 ? 1 : 0;
		
		log2 |= (num & 0xFFFFFFFF00000000L) != 0 ? 32 : 0;
		log2 |= (num & 0xFFFF0000FFFF0000L) != 0 ? 16 : 0;
		log2 |= (num & 0xFF00FF00FF00FF00L) != 0 ?  8 : 0;
		log2 |= (num & 0xF0F0F0F0F0F0F0F0L) != 0 ?  4 : 0;
		log2 |= (num & 0xCCCCCCCCCCCCCCCCL) != 0 ?  2 : 0;
		
		return log2;
		
	}
	
	public static int countBits(long num) {
		
		long c = num - ((num >>> 1) & 0x5555555555555555L);
		c = ((c >>>  2) & 0x3333333333333333L) + (c & 0x3333333333333333L);
		c = ((c >>>  4) + c) & 0x0F0F0F0F0F0F0F0FL;
		c = ((c >>>  8) + c) & 0x00FF00FF00FF00FFL;
		c = ((c >>> 16) + c) & 0x0000FFFF0000FFFFL;
		c = ((c >>> 32) + c) & 0x00000000FFFFFFFFL;
		
		return (int) c;
		
	}
	
	public static void sortDescending(int[] array) {
		
		boolean sorted = false;
		
		while(!sorted) {
			
			sorted = true;
			
			for(int i = 1; i < array.length; ++i) {
				
				if(array[i - 1] < array[i]) {
					
					sorted = false;
					
					int temp = array[i];
					array[i] = array[i - 1];
					array[i - 1] = temp;
					
				}
				
			}
			
		}
		
	}
	
}
