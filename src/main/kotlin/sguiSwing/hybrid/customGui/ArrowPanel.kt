package sguiSwing.hybrid.customGui

import rb.vectrix.mathUtil.round
import sgui.Direction.*
import sguiSwing.JColor
import sguiSwing.components.SJPanel
import java.awt.Graphics


class ArrowPanel(val bgcol: JColor?, val fgcol: JColor, val dir: sgui.Direction) : SJPanel() {
    init {
        background = null
        isOpaque = false
    }
    override fun paintComponent(g: Graphics) {
        if( bgcol != null) {
            g.color = bgcol
            g.fillRect(0, 0, width, height)
        }

        g.color = fgcol

        val w = width
        val h = height

        val logical_x = listOf( 0.05, 0.7, 0.7, 0.95, 0.7, 0.7, 0.05)
        val logical_y = listOf( 0.35, 0.35, 0.15, 0.5, 0.85, 0.65, 0.65)

        when( dir) {
            UP -> g.fillPolygon(
                    logical_y.map { w - (w * it).round }.toIntArray(),
                    logical_x.map { h - (h* it).round }.toIntArray(), 7)
            DOWN -> g.fillPolygon(
                    logical_y.map { (w * it).round }.toIntArray(),
                    logical_x.map { (h* it).round }.toIntArray(), 7)
            LEFT -> g.fillPolygon(
                    logical_x.map { w - (w * it).round }.toIntArray(),
                    logical_y.map { h - (h* it).round }.toIntArray(), 7)
            RIGHT -> g.fillPolygon(
                    logical_x.map { (w * it).round }.toIntArray(),
                    logical_y.map { (h* it).round }.toIntArray(), 7)
        }
    }
}