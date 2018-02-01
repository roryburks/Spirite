package spirite.base.util

import java.awt.*

/**
 * Created by Guy on 4/29/2017.
 */

object Colors {
    val BLACK = -0x1000000
    val DARK_GRAY = -0xbfbfc0
    val GRAY = -0x7f7f80
    val LIGHT_GRAY = -0x3f3f40
    val WHITE = -0x1
    val RED = -0x10000
    val BLUE = -0xffff01
    val GREEN = -0xff0100
    val CYAN = -0xff0001
    val MAGENTA = -0xff01
    val YELLOW = -0x100
    val ORANGE = -0x3800
    val PINK = -0x5051

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

    fun toColor(a: Int, r: Int, g: Int, b: Int): Int {
        return a and 0xFF shl 24 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    }

    fun toColor(r: Int, g: Int, b: Int): Int {
        return 0xFF shl 24 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    }

    fun darken(color: Color): Color {
        val hsv = FloatArray(3)
        Color.RGBtoHSB(color.red, color.green, color.blue, hsv)
        hsv[2] = Math.max(0f, hsv[2] - 0.1f)
        return Color(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]))
    }

    fun colorDistance(color1: Int, color2: Int): Double {
        val dr = getRed(color1) - getRed(color2)
        val dg = getGreen(color1) - getGreen(color2)
        val db = getBlue(color1) - getBlue(color2)
        val da = getAlpha(color1) - getAlpha(color2)
        return Math.sqrt((dr * dr + dg * dg + db * db + da * da).toDouble())
    }
}
