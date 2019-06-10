package rb.vectrix.calculate


// Wrapper to prevent expensive duplication of trigonometric calls
data class Angle( val theta: Double)
{
    val cos by lazy { kotlin.math.cos(theta) }
    val sin by lazy { kotlin.math.sin(theta) }
    val tan by lazy { sin/cos }
}