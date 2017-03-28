package mutil;

import java.util.ArrayList;

/***
 * A naive compact Data storage type designed for data which grows steadily
 * and then stops at a size that cannot be easily predicted.  Allocates in
 * CHUNKS rather than exponentially.
 * 
 * @author Rory Burks
 */
public class DataCompaction {
	public static class IntCompactor {
		private final int CHUNK_SIZE = 1024;
		private final ArrayList<int[]> data = new ArrayList<>();
		private int size = 0;
		
		public void add( int i) {
			if( size != Integer.MAX_VALUE){
				if( size % CHUNK_SIZE == 0) {
					data.add(new int[CHUNK_SIZE]);
				}
				
				data.get(size / CHUNK_SIZE)[size % CHUNK_SIZE] = i;
				size++;
			}
		}
		
		public int get( int n) {
			return data.get( n / CHUNK_SIZE)[n % CHUNK_SIZE];
		}
		
		public int size() {
			return size;
		}
	}
}
