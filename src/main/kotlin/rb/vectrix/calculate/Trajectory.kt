package rb.vectrix.calculate

import rb.vectrix.shapes.ParabolaD
import kotlin.math.*

object Trajectory
{
    fun buildArc( x: Double, y: Double, speed: Double, angle: Angle, gravity: Double, endX: Double) : ParabolaD {
        val const = gravity/(speed*speed*angle.cos*angle.cos)

        return ParabolaD.Make(const/2, -(x*const) + angle.tan, y + x*x*const/2 - angle.tan*x, x, endX)
    }

    fun calculateAngle(x: Double, y: Double, jumpSpeed: Double, targetX: Double, targetY: Double, gravity: Double)
            : Pair<Double,Double>?
    {
        val a = jumpSpeed
        val b = targetX
        val c = x
        val d = y
        val g = gravity
        val e = targetY

        val a2 = a*a
        val a4 = a2*a2
        val b2 = b*b
        val b3 = b*b2
        val b4 = b2*b2
        val c2 = c*c
        val c3 = c*c2
        val c4 = c2*c2
        val g2 = g*g

        // Courtesy of Wolfram Alpha solve e =tan(x)*(b-c)+g/2*(b-c)^2/(a^2+cos(x)^2)+d
        // Could probably be more optimized
        val denom = b4*g2 - 4*b3*c*g2 + 6*b2*c2*g2 - 4*b*c3*g2 + c4*g2
        val numerA = 2*a4*b2 + 2*a4*c2 - 4*a4*b*c - 2*a2*b2*d*g - 2*a2*c2*d*g + 4*a2*b*c*d*g + 2*a2*b2*e*g + 2*a2*c2*e*g - 4*a2*b*c*e*g

        val dinA = -4*a4*b2 + 8*a4*b*c - 4*a4*c2 + 4*a2*b2*d*g - 4*a2*b2*e*g - 8*a2*b*c*d*g + 8*a2*b*c*e*g + 4*a2*c2*d*g - 4*a2*c2*e*g
        val dinB = b4*g2 - 4*b3*c*g2 + 6*b2*c2*g2 - 4*b*c3*g2 + c4*g2
        val dinC = 4*a4*b2 - 8*a4*b*c + 4*a4*c2 + 4*a4*d*d - 8*a4*d*e + 4*a4*e*e

        val din = dinA*dinA - 4*dinB*dinC

        if( din < 0f)
            return null

        val toTry = listOf(
            (numerA + sqrt(din) /2)/denom,
            (numerA - sqrt(din) /2)/denom)

        val validResults =  toTry
            .mapNotNull {
                when {
                    it <= 0 -> null
                    else -> {
                        val inv = 1 / sqrt(it)
                        when {
                            inv < -1 || inv > 1 -> null
                            else -> inv
                        }
                    }
                }
            }
            .flatMap { inv -> listOf(acos(inv), acos(-inv),-acos(inv),-acos(-inv)) }
            .mapNotNull {
                // Remove negative time indices
                val t = (x - targetX) / (a* cos(it))
                if( t <= 0) null else Pair(it,t)
            }
            // Remove the non-solutions (there's probably a more mathematical filter for these)
            .filter { abs(x - targetX - (jumpSpeed * cos(it.first) * it.second)) < 0.001 }
            .filter { abs(targetY - y - (jumpSpeed * sin(it.first) * it.second + gravity / 2 * it.second * it.second)) < 0.001 }

        // Not sure why x is getting inverted
        // Get the one with the smallest and largest times
        val min = validResults.minBy{ it.second }?.run{ PI - first} ?: return null
        val max = validResults.maxBy{ it.second }?.run{ PI - first} ?: return null
        return Pair(min, max)

    }
}