package demonstration

import spirite.gui.Basic.SButton
import spirite.gui.IResizeContainerPanel.ContainerOrientation.HORIZONATAL
import spirite.gui.ResizeContainerPanel
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(ResizeContainerPanelDemoFrame())
}

class ResizeContainerPanelDemoFrame : JFrame() {
    init {
        layout = GridLayout()

        val centerButton = SButton("Stretch")
        val resize = ResizeContainerPanel( centerButton, HORIZONATAL)
        resize.minStretch = 100

        centerButton.action = {(-4..4).forEach {resize.getPanel(it)?.componentVisible = false}}

        resize.addPanel(SButton("1"), 100,100,-999)
        resize.addPanel(SButton("2"), 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(SButton("4"), 100,100,999)

        add( resize)
    }
}