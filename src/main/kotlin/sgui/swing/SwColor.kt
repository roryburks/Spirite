package sgui.swing

import rb.glow.ColorARGB32Normal
import rb.glow.SColor

typealias JColor = java.awt.Color

val SColor.jcolor get() = JColor(this.argb32, true)
val JColor.scolor: SColor get() = ColorARGB32Normal(this.rgb)