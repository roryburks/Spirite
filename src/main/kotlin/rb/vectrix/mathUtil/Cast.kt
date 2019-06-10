package rb.vectrix.mathUtil

import kotlin.math.roundToInt


inline val Int.f get() = this.toFloat()
inline val Int.d get() = this.toDouble()
inline val Int.b get() = this.toByte()
inline val Int.s get() = this.toShort()
inline val Int.l get() = this.toLong()

inline val Long.f : Float get() = this.toFloat()
inline val Long.d : Double get() = this.toDouble()
inline val Long.s : Short get() = this.toShort()
inline val Long.i : Int get() = this.toInt()

inline val Float.floor get() = kotlin.math.floor(this).toInt()
inline val Float.round get() = this.roundToInt()
inline val Float.ceil get() = kotlin.math.ceil(this).toInt()
inline val Float.d get() = this.toDouble()

inline val Double.floor get() = kotlin.math.floor(this).toInt()
inline val Double.round get() = this.roundToInt()
inline val Double.ceil get() = kotlin.math.ceil(this).toInt()
inline val Double.f get() = this.toFloat()

inline val Short.i get() = this.toInt()
inline val Short.d get() = this.toDouble()

inline val Byte.i get() = this.toInt()
inline val Byte.s get() = this.toShort()

inline val Boolean.sign get() = if( this) 1 else -1