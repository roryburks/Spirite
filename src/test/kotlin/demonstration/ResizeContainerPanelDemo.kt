package demonstration

import spirite.gui.ResizeContainerPanel
import spirite.gui.ResizeContainerPanel.ContainerOrientation.HORIZONATAL
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(ResizeContainerPanelDemoFrame())
}

class ResizeContainerPanelDemoFrame : JFrame() {
    init {
        layout = GridLayout()

        val centerButton = JButton("Stretch")
        val resize = ResizeContainerPanel( centerButton, HORIZONATAL)
        resize.minStretch = 100

        centerButton.addActionListener{(-4..4).forEach {resize.getPanel(it)?.componentVisible = false}}

        resize.addPanel(JButton("1"), 100,100,-999)
        resize.addPanel(JButton("2"), 100,100,-999)
        resize.addPanel(JButton("3"), 100,100,999)
        resize.addPanel(JButton("4"), 100,100,999)

        add( resize)
    }
}