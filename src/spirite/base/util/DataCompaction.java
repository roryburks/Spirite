package spirite.base.util;

import java.util.ArrayList;
import java.util.List;

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

        public int getChunkCount() { return data.size();}
        public int[] getChunk(int i) { return data.get(i);}
        public int getChunkSize(int i) {
            if( i < data.size()-1)
                return CHUNK_SIZE;
            return size % CHUNK_SIZE;
        }

        public int[] toArray() {
            int ret[] = new int[size];
            for(int i=0; i <data.size(); ++i) {
                System.arraycopy(data.get(i), 0, ret, CHUNK_SIZE*i, getChunkSize(i));
            }
            return ret;
        }
    }

    public static class FloatCompactor {
        private final int CHUNK_SIZE = 1024;
        private final ArrayList<float[]> data = new ArrayList<>();
        private int size = 0;

        public void add( float i) {
            if( size != Integer.MAX_VALUE){
                if( size % CHUNK_SIZE == 0) {
                    data.add(new float[CHUNK_SIZE]);
                }

                data.get(size / CHUNK_SIZE)[size % CHUNK_SIZE] = i;
                size++;
            }
        }

        public float get( int n) {
            return data.get( n / CHUNK_SIZE)[n % CHUNK_SIZE];
        }

        public int size() {
            return size;
        }

        public int getChunkCount() { return data.size();}
        public float[] getChunk(int i) { return data.get(i);}
        public int getChunkSize(int i) {
            if( i < data.size()-1)
                return CHUNK_SIZE;
            return size % CHUNK_SIZE;
        }

        public float[] toArray() {
            float ret[] = new float[size];
            for(int i=0; i <data.size(); ++i) {
                System.arraycopy(data.get(i), 0, ret, CHUNK_SIZE*i, getChunkSize(i));
            }
            return ret;
        }
    }
    
    public static class IntQueue {
        public static final int DEFAULT_SIZE = 10;
    	List<int[]> data = new ArrayList<>();
    	int front = 0;
    	int back = 0;
    	
    	public IntQueue() {}
    	
    	public void add( int n) {
    		if( data.isEmpty()) {
    			data.add( new int[DEFAULT_SIZE]);
    		}
    		else if( back == data.get(data.size()-1).length) {
    			data.add( new int[(data.size() == 1 ? data.get(0).length : data.get(data.size()-1).length*2)]);
    			back = 0;
    		}
    		data.get(data.size()-1)[back++] = n;
    	}
    	public int poll() {
    		int n = data.get(0)[front++];
    		if( front == data.get(0).length) {
    			data.remove(0);
    			front = 0;
    		}
    		return n;
    	}
    	public boolean isEmpty() {
    		return data.isEmpty() || (data.size() == 1 && front == back);
    	}
    }
}