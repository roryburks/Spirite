package spirite.base.util.compaction;

import java.util.ArrayList;

public class IntCompactor {
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