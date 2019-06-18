package sguiSwing

import rb.glow.color.ColorARGB32Normal
import rb.glow.color.SColor

typealias JColor = java.awt.Color

val SColor.jcolor get() = JColor(this.argb32, true)
val JColor.scolor: SColor get() = ColorARGB32Normal(this.rgb)