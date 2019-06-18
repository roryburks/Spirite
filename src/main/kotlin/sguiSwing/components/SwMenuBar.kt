package sguiSwing.components

import sguiSwing.mouseSystem.adaptMouseSystem
import sguiSwing.skin.Skin.BevelBorder.Dark
import sguiSwing.skin.Skin.Global.*
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.JMenuBar

class SwMenuBar : JMenuBar() {
    init {
        adaptMouseSystem()
        background = Fg.jcolor
        foreground = TextDark.jcolor
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, Dark.jcolor)
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val highlight = FgLight.jcolor
        val bg = Fg.jcolor

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