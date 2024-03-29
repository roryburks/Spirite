package demonstration

import sgui.core.Orientation.HORIZONTAL
import sgui.swing.components.ResizeContainerPanel
import sguiSwing.components.jcomponent
import spirite.sguiHybrid.Hybrid
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(ResizeContainerPanelDemoFrame())
}

class ResizeContainerPanelDemoFrame : JFrame() {
    init {
        layout = GridLayout()

        val centerButton = Hybrid.ui.Button("Click to Collapse All")
        val resize = ResizeContainerPanel(centerButton, HORIZONTAL)
        resize.minStretch = 100

        centerButton.action = {(-4..4).forEach {resize.getPanel(it)?.componentVisible = false}}

        resize.addPanel(Hybrid.ui.Button("1"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("2"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("3"), 100,100,999)
        resize.addPanel(Hybrid.ui.Button("4"), 100,100,999)

        add( resize.jcomponent)
    }
}