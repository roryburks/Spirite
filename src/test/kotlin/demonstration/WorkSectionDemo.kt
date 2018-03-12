package demonstration

import sjunit.TestConfig
import sjunit.TestHelper
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.basic.SButton
import spirite.gui.advanced.ResizeContainerPanel
import spirite.gui.major.work.WorkSection
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    exit()
    println("SADSA")
    DemoLauncher.launch(WorkSectionDemo())
}

class WorkSectionDemo : JFrame() {
    init {
        println("SADSA")
        val mockWs = TestHelper.makeShellWorkspace(200,200)

        layout = GridLayout()

        val ws = WorkSection()
        ws.currentWorkspace = mockWs
        val resize = ResizeContainerPanel(ws, HORIZONATAL, 200)

        resize.minStretch = 100

        val btn = SButton("1")
        btn.action = { println("S")}
        println("SADSA")

        resize.addPanel(btn, 100,100,-999)
        resize.addPanel(SButton("2"), 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(SButton("4"), 100,100,999)

        add( resize.component)
    }
}