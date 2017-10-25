package sjunit.util;

import org.junit.Test;

import spirite.base.util.compaction.DoubleEndedFloatCompactor;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.compaction.ReverseFloatCompactor;

public class CompactionTests {

	@Test
	public void TestReverseCompaction() {
		ReverseFloatCompactor rfc = new ReverseFloatCompactor();

		rfc.add(1);
		rfc.add(2);
		rfc.add(3);
		
		float[] result = rfc.toArray();
		
		assert( result[0] == 3);
		
		rfc = new ReverseFloatCompactor();
		for( int i=0; i < 9999; i++) {
			rfc.add(i);
		}
		result = rfc.toArray();
		
		for( int i=0; i < 9999; ++i) {
			assert( result[i] == 9998-i);
		}
	}
	
	@Test
	public void TestForwardCompaction( ) {
		FloatCompactor fc = new FloatCompactor();

		fc.add(1);
		fc.add(2);
		fc.add(3);
		
		float[] result = fc.toArray();
		
		assert( result[0] == 1);
		
		fc = new FloatCompactor();
		for( int i=0; i < 9999; i++) {
			fc.add(i);
		}
		result = fc.toArray();
		
		for( int i=0; i < 9999; ++i) {
			assert( result[i] == i);
		}
	}
	
	@Test
	public void TestSiameseCompaction( ) {
		ReverseFloatCompactor rfc = new ReverseFloatCompactor();
		FloatCompactor fc = new FloatCompactor();

		fc.add(1);
		rfc.add(-1);
		fc.add(2);
		rfc.add(-2);
		fc.add(3);
		rfc.add(-3);
		
		float[] result = new float[6];
		fc.insertIntoArray(result, 0);
		rfc.insertIntoArray(result, 3);
		
		assert( result[0] == 1);
		assert(result[5] == -1);
		
		final ReverseFloatCompactor rfc2 = new ReverseFloatCompactor();
		final FloatCompactor fc2 = new FloatCompactor();
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				for( int i=0; i < 9999; ++i)
					fc2.add(i);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				for( int i=0; i < 9999; ++i)
					rfc2.add(-i);
			}
		});
		t1.start();
		t2.start();
		try {
			synchronized(t1) {
				t1.wait();
			}
			synchronized(t2) {
				t2.wait();
		    }
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		

		result = new float[9999*2];
		fc2.insertIntoArray(result, 0);
		rfc2.insertIntoArray(result, 9999);
		for( int i=0; i < 9999; ++i) {
			assert( result[i] == i);
		}
		for( int i=0; i < 9999; ++i) {
			assert( result[9999+9999-1-i] == -i);
		}
	}
	
	@Test
	public void TestDoubleEndedCompaction() {
		DoubleEndedFloatCompactor defc = new DoubleEndedFloatCompactor();
		
		defc.addHead(1);
		defc.addHead(2);
		defc.addHead(3);
		defc.addTail(-1);
		defc.addTail(-2);
		defc.addTail(-3);
		
		float[] result = new float[6];
		defc.insertIntoArray(result, 0);

		assert(result[0] == -3);
		assert(result[5] == 3);
		assert(result[2] == -1);
		
		defc = new DoubleEndedFloatCompactor();
		
		for( int i=0; i<9999; ++i) {
			defc.addHead(i);
			defc.addTail(-i);
		}
		result = new float[9999*2];
		defc.insertIntoArray(result, 0);
		for( int i=0; i<9999*2; ++i) {
			System.out.println(i + "," + result[i]);
			if( i < 9999)
				assert( result[i] == -(9998-i));
			else
				assert( result[i] == i - 9999);
		}
	}
}
