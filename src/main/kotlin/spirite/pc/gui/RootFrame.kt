package spirite.pc.gui

import javax.swing.JFrame

import spirite.base.brains.MasterControl
import spirite.gui.ResizeContainerPanel
import spirite.gui.ResizeContainerPanel.ContainerOrientation.HORIZONATAL
import java.awt.GridLayout
import javax.swing.JButton

class RootFrame(
        val master: MasterControl
    ) : JFrame()
{
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