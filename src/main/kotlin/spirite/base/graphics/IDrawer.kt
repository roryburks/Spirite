package spirite.base.graphics

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.util.Color

/***
 * You can think of IDrawer as somewhat of an extension to GraphicsContent as it takes a base surface/image and applies
 * certain graphical functions on them, but it doesn't have the same degree of parameters and settings.  Instead it
 * has very simple and straightfoward actions like "Invert image", "Change jcolor", etc.
 */
interface IDrawer {
    fun invert()
    fun changeColor(from: Color, to: Color, mode: ColorChangeMode)
    fun fill( x: Int, y: Int, color: Color)
}