package rb.vectrix.mathUtil

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MathUtil {
    fun packInt(high: Int, low: Int) = high and 0xffff shl 16 or (low and 0xffff)
    fun low16(i: Int) = i and 0xffff
    fun high16(i: Int) = i.ushr(16)

    fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).d).f
    }

    fun clip(min: Int, value: Int, max: Int): Int {
        if (value < min) return min
        return if (value > max) max else value
    }
    fun clip(min: Float, value: Float, max: Float): Float {
        if (value < min) return min
        return if (value > max) max else value
    }
    fun clip(min: Double, value: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }

    fun cycle( start: Int, end: Int, t: Int) = when( val diff = end - start){
        0 -> 0
        else -> ((t - start) % diff + diff) % diff + start
    }
    fun cycle(start: Float, end: Float, t: Float) = when( val diff = end - start){
        0.0f -> 0.0f
        else -> ((t - start) % diff + diff) % diff + start
    }
    fun cycle(start: Double, end: Double, t: Double): Double = when( val diff = end - start) {
        0.0 -> 0.0
        else -> ((t - start) % diff + diff) % diff + start
    }

    fun lerp( min: Float, max: Float, t:Float) = min + (max - min)*t
    fun lerp( min: Double, max: Double, t:Double) = min + (max - min)*t
    fun derp( minT: Float, maxT: Float, t:Float) = (t - minT)/(maxT-minT)
    fun derp( minT: Double, maxT: Double, t:Double) = (t - minT)/(maxT-minT)
    fun lerpyDerp(min: Float, max:Float, lowT: Float, highT: Float, t: Float) = min + (max-min)*(t-lowT)/(highT-lowT)
    fun lerpyDerp(min: Double, max:Double, lowT: Double, highT: Double, t: Double) = min + (max-min)*(t-lowT)/(highT-lowT)

    //region minOrNull
    fun minOrNull(a: Int?, b: Int?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> min(a, b)
    }
    fun minOrNull(a: Float?, b: Float?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> min(a, b)
    }

    fun minOrNull(a: Float?, b: Float?, c:Float?) = when {
        a == null -> when {
            b == null -> when {
                c == null -> null
                else -> c
            }
            c == null -> b
            else -> min(b, c)
        }
        b == null -> when {
            c == null -> a
            else -> min(a, c)
        }
        else -> when {
            c == null -> min(a, b)
            else -> minOf(a,b,c)
        }
    }

    fun minOrNull(a: Double?, b: Double?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> min(a, b)
    }

    fun minOrNull(a: Double?, b: Double?, c:Double?) = when {
        a == null -> when {
            b == null -> when {
                c == null -> null
                else -> c
            }
            c == null -> b
            else -> min(b, c)
        }
        b == null -> when {
            c == null -> a
            else -> min(a, c)
        }
        else -> when {
            c == null -> min(a, b)
            else -> minOf(a,b,c)
        }
    }
    // endregion

    // region maxOrNull
    fun maxOrNull(a: Int?, b: Int?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> max(a, b)
    }
    fun maxOrNull(a: Float?, b: Float?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> max(a, b)
    }

    fun maxOrNull(a: Float?, b: Float?, c:Float?) = when {
        a == null -> when {
            b == null -> when {
                c == null -> null
                else -> c
            }
            c == null -> b
            else -> max(b, c)
        }
        b == null -> when {
            c == null -> a
            else -> max(a, c)
        }
        else -> when {
            c == null -> max(a, b)
            else -> maxOf(a,b,c)
        }
    }

    fun maxOrNull(a: Double?, b: Double?) = when {
        a == null -> when {
            b == null -> null
            else -> b
        }
        b == null -> a
        else -> max(a, b)
    }

    fun maxOrNull(a: Double?, b: Double?, c:Double?) = when {
        a == null -> when {
            b == null -> when {
                c == null -> null
                else -> c
            }
            c == null -> b
            else -> max(b, c)
        }
        b == null -> when {
            c == null -> a
            else -> max(a, c)
        }
        else -> when {
            c == null -> min(a, b)
            else -> maxOf(a,b,c)
        }
    }
}