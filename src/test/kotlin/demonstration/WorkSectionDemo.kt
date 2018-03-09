package demonstration

import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.basic.SButton
import spirite.gui.advanced.ResizeContainerPanel
import spirite.gui.major.work.WorkSection
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(WorkSectionDemo())
}

class WorkSectionDemo : JFrame() {
    init {
        layout = GridLayout()

        val resize = ResizeContainerPanel(WorkSection(), HORIZONATAL, 200)

        resize.minStretch = 100

        resize.addPanel(SButton("1"), 100,100,-999)
        resize.addPanel(SButton("2"), 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(SButton("4"), 100,100,999)

        add( resize)
    }
}