package rb.glow

import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.Vec4f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.round
import kotlin.math.sqrt


// TODO: Eventually I will need a more robust color space... space.
sealed class Color {
    val rgbComponent : Vec3f by lazy { Vec3f(red, green, blue) }
    val rgbaComponent : Vec4f by lazy { Vec4f(red, green, blue, alpha) }
    abstract val argb32: Int
    abstract val red : Float
    abstract val green: Float
    abstract val blue: Float
    abstract val alpha: Float
}

abstract class ColorARGB32(val argb: Int)
    : Color()
{
    override val argb32: Int get() = argb

    val a: Int get() = argb ushr 24
    val r: Int get() = (argb ushr 16) and 0xff
    val g: Int get() = (argb ushr 8) and 0xff
    val b: Int get() = argb and 0xff
}

class ColorARGB32Normal(argb: Int)
    : ColorARGB32(argb)
{
    override val red: Float get() = (r / 255.0f)
    override val green: Float get() = (g / 255.0f)
    override val blue: Float get() = (b / 255.0f)
    override val alpha: Float get() = (a / 255.0f)

    companion object {
        fun FromComponents( alpha: Float, red: Float, green: Float, blue: Float) : ColorARGB32Normal {
            val a = MathUtil.clip(0, (alpha*255).round, 255)
            val r = MathUtil.clip(0, (red*255).round, 255)
            val g = MathUtil.clip(0, (green*255).round, 255)
            val b = MathUtil.clip(0, (blue*255).round, 255)

            return ColorARGB32Normal((a shl 24) or (r shl 16) or (g shl 8) or (b shl 0))
        }
    }
}
fun Int.toColor() = ColorARGB32Normal(this)
fun Int.toColorPremultiplied() = ColorARGB32Premultiplied(this)

class ColorARGB32Premultiplied(argb: Int)
    : ColorARGB32(argb)
{
    override val red: Float get() = if( alpha == 0f) 0f else (r/255.0f) / alpha
    override val green: Float get() = if( alpha == 0f) 0f else (g/255.0f) / alpha
    override val blue: Float get() = if( alpha == 0f) 0f else (b/255.0f) / alpha
    override val alpha: Float get() = (a/255.0f)
}

private class ColorTransparent : Color() {
    override val argb32: Int get() = 0
    override val red: Float get() = 0f
    override val green: Float get() = 0f
    override val blue: Float get() = 0f
    override val alpha: Float get() = 0f
}

object Colors {
    val BLACK = ColorARGB32Normal(0xFF000000.toInt())
    val DARK_GRAY = ColorARGB32Normal(0xFF404040.toInt())
    val GRAY = ColorARGB32Normal(0xFF808080.toInt())
    val LIGHT_GRAY = ColorARGB32Normal(0xFFC0C0C0.toInt())
    val WHITE = ColorARGB32Normal(0xFFFFFFFF.toInt())
    val RED = ColorARGB32Normal(0xFFFF0000.toInt())
    val BLUE = ColorARGB32Normal(0xFF0000FF.toInt())
    val GREEN = ColorARGB32Normal(0xFF00FF00.toInt())
    val CYAN = ColorARGB32Normal(0xFF00FFFF.toInt())
    val MAGENTA = ColorARGB32Normal(0xFFFF00FF.toInt())
    val YELLOW = ColorARGB32Normal(0xFFFFFF00.toInt())
    val ORANGE = ColorARGB32Normal(0xFFFFC800.toInt())
    val PINK = ColorARGB32Normal(0xFFFFAFAF.toInt())

    val TRANSPARENT : Color = ColorTransparent()

    fun getAlpha(argb: Int): Int {
        return argb.ushr(24) and 0xFF
    }

    fun getRed(argb: Int): Int {
        return argb.ushr(16) and 0xFF
    }

    fun getGreen(argb: Int): Int {
        return argb.ushr(8) and 0xFF
    }

    fun getBlue(argb: Int): Int {
        return argb and 0xFF
    }

    fun toColor(a: Int, r: Int, g: Int, b: Int): Color {
        return ColorARGB32Normal(a and 0xFF shl 24 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF))
    }
//
//    fun toColor(r: Int, g: Int, b: Int): Int {
//        return 0xFF shl 24 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
//    }
//
//    fun darken(jcolor: java.awt.Color): java.awt.Color {
//        val hsv = FloatArray(3)
//        java.awt.Color.RGBtoHSB(jcolor.red, jcolor.green, jcolor.blue, hsv)
//        hsv[2] = Math.max(0f, hsv[2] - 0.1f)
//        return java.awt.Color(java.awt.Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]))
//    }
//
//    fun colorDistance(color1: Int, color2: Int): Double {
//        val dr = getRed(color1) - getRed(color2)
//        val dg = getGreen(color1) - getGreen(color2)
//        val db = getBlue(color1) - getBlue(color2)
//        val da = getAlpha(color1) - getAlpha(color2)
//        return Math.sqrt((dr * dr + dg * dg + db * db + da * da).toDouble())
//    }
}

object ColorUtil {
    fun colorDistance(c1: Color, c2: Color) : Double {
        // I don't have to explain.  sqrt(2)^2 + 1^2 = c^2, c = sqrt(3), d = sqrt(4) = 2
        val dr = (c1.red - c2.red) * 255.0/2
        val dg = (c1.green - c2.green) * 255.0/2
        val db = (c1.blue - c2.blue) * 255.0/2
        val da = (c1.alpha - c2.alpha) * 255.0/2
        return sqrt(dr*dr+dg*dg+db*db+da*da)
    }
}
