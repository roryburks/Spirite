package spirite.pc.gui

import sgui.generic.color.ColorARGB32Normal
import sgui.generic.color.SColor

typealias JColor = java.awt.Color

val SColor.jcolor get() = JColor(this.argb32, true)
val JColor.scolor: SColor get() = ColorARGB32Normal(this.rgb)