package rb.vectrix.functions

// TODO: Maybe put this in rb.vectrix, maybe add the two current implementation (x^p, and a segmented linear) to possible
//  implementations?

/***
 * An InvertibleFunction is a function such that f(f_inv(x)) = x.  Since we are dealing with floats, that is not a strong
 * Contract, but in order for the UI-based consumers to behave "correctly" (at least to the eye), this behavior should
 * be attempted in approximal.
 */
interface InvertibleFunction<T> {
    fun perform( x : Float) : Float
    fun invert( x: Float) : Float
}