package spirite.pc.gui.basic

import spirite.gui.resources.Skin.BevelBorder.Dark
import spirite.gui.resources.Skin.Global.*
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.JMenuBar

class SwMenuBar : JMenuBar() {
    init {
        background = Fg.color
        foreground = TextDark.color
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, Dark.color)
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val highlight = FgLight.color
        val bg = Fg.color

        val w = width
        val h = height

        val g2 = g.create() as Graphics2D
        g2.color = bg
        g2.fillRect(0, 0, w, h)

        g2.paint = GradientPaint(
                Point(0, h / 3 - h / 5),
                bg,
                Point(0, h / 3),
                highlight)
        g2.fillRect(0, h / 5, width, h / 5)
        g2.paint = GradientPaint(
                Point(0, h / 3),
                highlight,
                Point(0, h / 3 + h / 3),
                bg)
        g2.fillRect(0, h / 3, width, h / 3)
        g2.dispose()

        super.paintComponent(g)
    }
}