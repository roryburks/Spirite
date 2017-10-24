//package spirite.base.util.compaction;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//
//public class DoubleEndedFloatCompactor {
//    private final int CHUNK_SIZE = 1024;
//    int tail = CHUNK_SIZE/2-1;
//    int head = CHUNK_SIZE/2;
//    private final LinkedList<float[]> data = new LinkedList<float[]>();
//
//    public void addHead( float i) {
//        if( head == CHUNK_SIZE) {
//            data.addLast(new float[CHUNK_SIZE]);
//            head = 0;
//        }
//        data.getLast()[head] = i;
//        head++;
//    }
//    public void addTail( float i) {
//    	if( tail == -1) {
//    		data.addFirst(new float[CHUNK_SIZE]);
//    		tail = CHUNK_SIZE-1;
//    	}
//    	data.getFirst()[tail] = i;
//    	tail--;
//    }
//
//    public int size() {
//        return (data.size() == 1) ? head - tail - 1 :
//        	;
//    }
//
//    public int getChunkCount() { return data.size();}
//    public float[] getChunk(int i) { return data.get(i);}
//
//    public float[] toArray() {
//        float ret[] = new float[size];
//        for(int i=0; i <data.size(); ++i) {
//            System.arraycopy(data.get(i), 0, ret, CHUNK_SIZE*i, getChunkSize(i));
//        }
//        return ret;
//    }
//    
//    public void insertIntoArray( float[] array, int start) {
//        for(int i=0; i <data.size(); ++i) {
//            System.arraycopy(data.get(i), 0, array, start + CHUNK_SIZE*i, getChunkSize(i));
//        }
//    }
//}