package spirite.base.util.compaction;

import java.util.ArrayList;

public class ReverseFloatCompactor {
    private final int CHUNK_SIZE = 1024;
    private final ArrayList<float[]> data = new ArrayList<>();
    private int size = 0;

    public void add( float i) {
        if( size != Integer.MAX_VALUE){
            if( size % CHUNK_SIZE == 0) {
                data.add(new float[CHUNK_SIZE]);
            }

            data.get(size / CHUNK_SIZE)[CHUNK_SIZE - (size % CHUNK_SIZE) - 1] = i;
            size++;
        }
    }

    public float get( int n) {
        return data.get( n / CHUNK_SIZE)[CHUNK_SIZE - (n % CHUNK_SIZE) - 1];
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
        if( size == 0) return ret;
        
        int leadingOffset = getChunkSize(data.size()-1);
        System.arraycopy(data.get(data.size()-1), CHUNK_SIZE - leadingOffset, ret, 0, leadingOffset);
        
        for(int i=1; i < data.size(); ++i) {
        	int index = data.size() - i - 1;
            System.arraycopy(data.get(index), 0, ret, CHUNK_SIZE*(i-1)+leadingOffset, CHUNK_SIZE);
        }
        return ret;
    }
    public void insertIntoArray( float[] array, int start) {
        if( size == 0) return;
        
        int leadingOffset = getChunkSize(data.size()-1);
        System.arraycopy(data.get(data.size()-1), CHUNK_SIZE - leadingOffset, array, start, leadingOffset);
        
        for(int i=1; i < data.size(); ++i) {
        	int index = data.size() - i - 1;
            System.arraycopy(data.get(index), 0, array, start + CHUNK_SIZE*(i-1)+leadingOffset, CHUNK_SIZE);
        }
    
    }
}