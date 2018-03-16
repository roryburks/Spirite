package demonstration

import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.advanced.crossContainer.CrossContainer
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.jcomponent
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(CrossContainerDemo())
}

class CrossContainerDemo : JFrame() {
    init {
        layout = GridLayout()

        val x = CrossContainer {
            rows += {
                add(Hybrid.ui.Button("wac"), flex = 1f)
                add(Hybrid.ui.Button("vscroll"), width = 12)
                flex = 1f
            }
            rows += {
                add(Hybrid.ui.Button("hscroll"), flex = 1f)
                add(Hybrid.ui.Button("zoom"), width = 12)
                height = 12
            }
            rows += {
                add(Hybrid.ui.Label("CoordinateLabel"))
                addGap(0, 3, Int.MAX_VALUE)
                add(Hybrid.ui.Label("MessageLabel"))
                height = 24
            }
        }
        val resize = ResizeContainerPanel(x, HORIZONATAL)
        resize.minStretch = 100

        val resize2 = ResizeContainerPanel(Hybrid.ui.Button("A"), VERTICAL)
        resize2.addPanel(Hybrid.ui.Button("B"), 20, 20, -100)
        resize2.addPanel(Hybrid.ui.Button("C"), 20, 20, 100)

        resize.addPanel(Hybrid.ui.Button("1"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("2"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("3"), 100,100,999)
        resize.addPanel(resize2, 100,100,999)

        add( resize.jcomponent)
    }
}