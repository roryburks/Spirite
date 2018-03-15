package demonstration

import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.advanced.ResizeContainerPanel
import spirite.hybrid.Hybrid
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(ResizeContainerPanelDemoFrame())
}

class ResizeContainerPanelDemoFrame : JFrame() {
    init {
        layout = GridLayout()

        val centerButton = Hybrid.ui.Button("Click to Collapse All")
        val resize = ResizeContainerPanel(centerButton, HORIZONATAL)
        resize.minStretch = 100

        centerButton.action = {(-4..4).forEach {resize.getPanel(it)?.componentVisible = false}}

        resize.addPanel(Hybrid.ui.Button("1"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("2"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("3"), 100,100,999)
        resize.addPanel(Hybrid.ui.Button("4"), 100,100,999)

        add( resize.component)
    }
}