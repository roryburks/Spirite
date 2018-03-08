package spirite.base.util

interface InvertibleFunction<T> {
    fun perform( x : Float) : Float
    fun invert( x: Float) : Float
}