package rb.vectrix.calculate

// TODO: Figure out where this belongs.

/** Given a sorted, increasing list of floats, returns an approximate index of
* the given float's position in the array.
*
* @return Will be in [0,increasing.length-1]. Math.round(ret) will be the closest match.
* Math.floor/Math.ceil can be used to find left/right nearest.
*/
fun nearestBinarySearch(increasing:FloatArray, toFind:Float):Float {
	val t = increasing
	val length = t.size
	
	if (toFind < 0)
		return 0f
	
	var min = 0
	var max = length - 1
	var mid : Int
	while (min <= max)
	{
		mid = min + ((max - min) / 2)
		if (t[mid] > toFind)
			max = mid - 1
		else if (t[mid] < toFind)
			min = mid + 1
		else
			return mid.toFloat()
	}
	
	return when {
		min >= length	-> length-1f
		min == 0 		-> 0f
		else  			-> (toFind - t[min - 1]) / (t[min] - t[min - 1]) + (min-1)
	}
}