package sgui.generic

import java.awt.Graphics
import java.awt.Rectangle

object UIUtil {


    /***
     * Draws the string centered in the given Rectangle (using the font already
     * set up in the Graphics)
     */
    fun drawStringCenter(g: Graphics, text: String, rect: Rectangle) {
        val fm = g.fontMetrics
        val dx = (rect.width - fm.stringWidth(text)) / 2
        val dy = (rect.height - fm.height) / 2 + fm.ascent
        g.drawString(text, dx, dy)
    }
}