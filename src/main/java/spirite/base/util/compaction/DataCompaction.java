package spirite.base.util.compaction;

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