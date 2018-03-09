package demonstration

import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.advanced.crossContainer.CrossContainer
import spirite.gui.basic.SButton
import spirite.gui.advanced.ResizeContainerPanel
import spirite.gui.basic.SLabel
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
                add(SButton("wac"), flex = 1f)
                add(SButton("vscroll"), width = 12)
                flex = 1f
            }
            rows += {
                add(SButton("hscroll"), flex = 1f)
                add(SButton("zoom"), width = 12)
                height = 12
            }
            rows += {
                add(SLabel("CoordinateLabel"))
                addGap(0, 3, Int.MAX_VALUE)
                add(SLabel("MessageLabel"))
                height = 24
            }
        }
        val resize = ResizeContainerPanel(x, HORIZONATAL)
        resize.minStretch = 100

        val resize2 = ResizeContainerPanel( SButton("A"), VERTICAL)
        resize2.addPanel(SButton("B"), 20, 20, -100)
        resize2.addPanel(SButton("C"), 20, 20, 100)

        resize.addPanel(SButton("1"), 100,100,-999)
        resize.addPanel(SButton("2"), 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(resize2, 100,100,999)

        add( resize)
    }
}