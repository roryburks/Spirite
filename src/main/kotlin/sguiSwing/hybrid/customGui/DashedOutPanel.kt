package sguiSwing.hybrid.customGui

import sguiSwing.JColor
import java.awt.Graphics
import javax.swing.JPanel

public class DashedOutPanel(val bgcol: JColor?, val fgcol: JColor) : JPanel() {
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
        (0.. (width + height)/4)
                .forEach { g.drawLine(0, it*4, it*4, 0)}
    }
}