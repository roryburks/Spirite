package spirite.gui

import spirite.base.util.Color
import spirite.base.util.Colors

fun Color.toJColor() : java.awt.Color {
    val argb32 = this.argb32
    return java.awt.Color( Colors.getRed(argb32), Colors.getGreen(argb32), Colors.getGreen(argb32), Colors.getAlpha(argb32))
}