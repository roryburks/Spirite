package spirite.pc.gui

import spirite.base.util.ColorARGB32
import spirite.base.util.ColorARGB32Normal

val spirite.base.util.Color.jcolor get() = java.awt.Color(this.argb32, true)
val  java.awt.Color.scolor get() = ColorARGB32Normal( this.rgb)