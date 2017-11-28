package spirite.base.util;

public class BinSearch {
	
	/** Given a sorted, increasing list of floats, returns an approximate index of
	 * the given float's position in the array.
	 * 
	 * @return Will be in [0,increasing.length-1].  Math.round(ret) will be the closest match.
	 * Math.floor/Math.ceil can be used to find left/right nearest.
	 */
	public static float ApproximateBinarySearch(float[] increasing, float toFind) {
    	float[] t= increasing;
    	int length = t.length;
    	
		if( toFind < 0) return 0;
		
		int min=0;
		int max=length-1;
		int mid = 0;
		
		while( min <= max) {
			mid = min + ((max-min)/2);
			
			if( t[mid] > toFind)
				max = mid-1;
			else if( t[mid] < toFind)
				min = mid+1;
			else
				return mid;
		}
		
		if( min >= length)
			return length-1;
		if( min == 0)
			return 0;
		
		float lerp = (toFind-t[min-1])/(float)(t[min]-t[min-1]);
		
		if( lerp < 0) return min-1;
		if( lerp > 1) return min+1;
		return (min-1) + lerp;
	}
}
