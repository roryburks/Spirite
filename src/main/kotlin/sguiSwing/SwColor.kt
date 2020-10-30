package sguiSwing

import rb.glow.ColorARGB32Normal
import rbJvm.glow.SColor

typealias JColor = java.awt.Color

val SColor.jcolor get() = JColor(this.argb32, true)
val JColor.scolor: SColor get() = ColorARGB32Normal(this.rgb)