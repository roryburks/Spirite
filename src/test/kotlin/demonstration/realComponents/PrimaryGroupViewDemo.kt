package demonstration.realComponents

import demonstration.DemoLauncher
import sjunit.TestHelper
import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.gui.components.major.groupView.GroupView
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.jcomponent
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

private lateinit var master: MasterControl
private lateinit var ws: GroupView

fun main( args: Array<String>) {

    SwingUtilities.invokeLater {
        master = MasterControl()
        val mockWs = TestHelper.makeShellWorkspace(200,200, renderEngine = master.renderEngine, strokeProvider = master.strokeDrawerProvider)
        master.workspaceSet.addWorkspace(mockWs)
        val wsd = PrimaryGroupViewDemo()
        DemoLauncher.launch(wsd, 800, 600)
    }
}

class PrimaryGroupViewDemo : JFrame() {
    init {

        layout = GridLayout()

        //SwingUtilities.invokeLater {
        //}

        ws = GroupView(master)
        val resize = ResizeContainerPanel(ws, HORIZONTAL, 200)

        resize.minStretch = 100

        val btn = Hybrid.ui.Button("AddLayer")
        btn.action = {
            master.workspaceSet.currentWorkspace?.groupTree?.addNewSimpleLayer(null, "Layer", FLAT, 10, 10)
        }
        val btn2 = Hybrid.ui.Button("AddGroup")
        btn2.action = {
            master.workspaceSet.currentWorkspace?.groupTree?.addGroupNode(null, "Group")
        }

        val btn3 = Hybrid.ui.Button("Undo")
        btn3.action = { master.workspaceSet.currentWorkspace?.undoEngine?.undo()}
        val btn4 = Hybrid.ui.Button("Redo")
        btn4.action = { master.workspaceSet.currentWorkspace?.undoEngine?.redo()}

        resize.addPanel(btn, 100,100,-999)
        resize.addPanel(btn2, 100,100,-999)
        resize.addPanel(btn3, 100,100,999)
        resize.addPanel(btn4, 100,100,999)

        add( resize.jcomponent)
    }
}