package spirite.base.util.compaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class DoubleEndedFloatCompactor {
    private final int CHUNK_SIZE = 1024;
    int tail = CHUNK_SIZE/2-1;
    int head = CHUNK_SIZE/2;
    private final LinkedList<float[]> data = new LinkedList<float[]>();
    
    public DoubleEndedFloatCompactor() {
    	data.add(new float[CHUNK_SIZE]);
    }

    public void addHead( float i) {
        if( head == CHUNK_SIZE) {
            data.addLast(new float[CHUNK_SIZE]);
            head = 0;
        }
        data.getLast()[head] = i;
        head++;
    }
    public void addTail( float i) {
    	if( tail == -1) {
    		data.addFirst(new float[CHUNK_SIZE]);
    		tail = CHUNK_SIZE-1;
    	}
    	data.getFirst()[tail] = i;
    	tail--;
    }

    public int size() {
        return (data.size() == 1) ? head - tail - 1 :
        	(data.size() - 2)*CHUNK_SIZE + (head) + (CHUNK_SIZE-tail);
    }
    //public int get(i) 

    public int getChunkCount() { return data.size();}
    //public float[] getChunk(int i) { return data.get(i);}

    public float[] toArray() {
        float ret[] = new float[size()];
        insertIntoArray(ret, 0);
        return ret;
    }
    
    public void insertIntoArray( float[] array, int start) {
        if(data.size() == 1) 
        	System.arraycopy(data.getFirst(), tail+1, array, start, head-tail-1);
        else {
        	Iterator<float[]> it = data.iterator();
        	int caret = start;
        	// First
        	System.arraycopy(it.next(), tail+1, array, caret, CHUNK_SIZE-tail-1);
        	caret += CHUNK_SIZE - tail-1;
        	while( it.hasNext()) {
        		float[] chunk = it.next();
        		if(it.hasNext()) {
        			// Middle
        			System.arraycopy(chunk, 0, array, caret, CHUNK_SIZE);
        			caret+=CHUNK_SIZE;
        		}
        		else {
        			// Last
        			System.arraycopy(chunk, 0, array, caret, head);
        		}
        		
        	}
        }
    }
}