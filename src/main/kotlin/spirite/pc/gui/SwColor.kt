package spirite.pc.gui

import spirite.base.util.ColorARGB32Normal

typealias SColor = spirite.base.util.Color
typealias JColor = java.awt.Color

val SColor.jcolor get() = JColor(this.argb32, true)
val JColor.scolor: SColor get() = ColorARGB32Normal( this.rgb)