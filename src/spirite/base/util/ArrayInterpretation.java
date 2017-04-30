package spirite.base.util;


/**
 * This namespace provides functions for reinterpretting Array Address
 * space through the length and get methods without allocating any new data.
 */
public class ArrayInterpretation {

	public static interface InterpretedArray<T> {
		public int length();
		public T get( int i);
	}
	public static  class StandardArray<T> implements InterpretedArray<T> {
		private final T[] array;
		StandardArray( T[] array) {
			this.array = array;
		}
		@Override public int length() {
			return array.length;
		}
		@Override public T get(int i) {
			return array[i];
		}
	}
	public static  class BackwardsArray<T> implements InterpretedArray<T> {
		private final T[] array;
		BackwardsArray( T[] array) {
			this.array = array;
		}
		@Override public int length() {
			return array.length;
		}
		@Override public T get(int i) {
			return array[array.length-1-i];
		}
	}
	public static class LimitedArray<T> implements InterpretedArray<T> {
		private final T[] array;
		private final int min, max;
		private final boolean backwards;
		LimitedArray( T[] array, int start, int end) {
			if( start < 0 || end < 0 || start > array.length || end > array.length)
				throw new ArrayIndexOutOfBoundsException( "start:"+start+",end:"+end);
			
			backwards = (end < start);
			
			this.min = Math.min(start, end);
			this.max = Math.max(start, end);
			this.array = array;
		}
		@Override public int length() {
			return max - min + 1;
		}
		@Override public T get(int i) {
			if( i < 0 || i >= max-min + 1) throw new ArrayIndexOutOfBoundsException( "i:" + i +": min" + min + " , max:" + max);
			
			if( backwards) 
				return array[ max - i];
			else 
				return array[ min + i];
		}
	}
	

	public static interface InterpretedIntArray {
		public int length();
		public int get( int i);
	}
	public static class IntCounter implements InterpretedIntArray {
		private final int min, max;
		private final boolean backwards;
		public IntCounter(  int start, int end) {
			backwards = (end < start);
			
			this.min = Math.min(start, end);
			this.max = Math.max(start, end);
		}
		@Override public int length() {
			return max - min + 1;
		}
		@Override public int get(int i) {
			if( i < 0 || i >= max-min + 1) throw new ArrayIndexOutOfBoundsException( "i:" + i +": min" + min + " , max:" + max);
			
			if( backwards) 
				return max - i;
			else 
				return min + i;
		}
	}

	public static interface InterpretedBooleanArray {
		public int length();
		public boolean get( int i);
	}
	public static  class StandardBoolArray implements InterpretedBooleanArray {
		private final boolean[] array;
		StandardBoolArray( boolean[] array) {
			this.array = array;
		}
		@Override public int length() {
			return array.length;
		}
		@Override public boolean get(int i) {
			return array[i];
		}
	}
	public static class BackwardsBoolArray implements InterpretedBooleanArray {
		private final boolean[] array;
		BackwardsBoolArray( boolean[] array) {
			this.array = array;
		}
		@Override public int length() {
			return array.length;
		}
		@Override public boolean get(int i) {
			return array[array.length-1-i];
		}
	}
	public static class LimitedBoolArray implements InterpretedBooleanArray {
		private final boolean[] array;
		private final int min, max;
		private final boolean backwards;
		LimitedBoolArray( boolean[] array, int start, int end) {
			if( start < 0 || end < 0 || start > array.length || end > array.length)
				throw new ArrayIndexOutOfBoundsException( "start:"+start+",end:"+end);
			
			backwards = (end < start);
			
			this.min = Math.min(start, end);
			this.max = Math.max(start, end);
			this.array = array;
		}
		@Override public int length() {
			return max - min + 1;
		}
		@Override public boolean get(int i) {
			if( i < 0 || i >= max-min + 1) throw new ArrayIndexOutOfBoundsException( "i:" + i +": min" + min + " , max:" + max);
			
			if( backwards) 
				return array[ max - i];
			else 
				return array[ min + i];
		}
	}
}
