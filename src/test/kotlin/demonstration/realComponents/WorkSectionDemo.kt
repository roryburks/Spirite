package demonstration.realComponents

import demonstration.DemoLauncher
import sjunit.TestHelper
import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.gui.views.work.WorkTabPane
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.jcomponent
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

private lateinit var master: MasterControl
private lateinit var ws: WorkTabPane

fun main( args: Array<String>) {

    SwingUtilities.invokeLater {
        master = MasterControl()
        val wsd = WorkSectionDemo()
        DemoLauncher.launch(wsd, 800, 600)
    }
}

class WorkSectionDemo : JFrame() {
    init {

        layout = GridLayout()

        //SwingUtilities.invokeLater {
        //}

        ws = WorkTabPane(master)
        val resize = ResizeContainerPanel(ws.component, HORIZONTAL, 200)

        resize.minStretch = 100

        val btn = Hybrid.ui.Button("New Workspace")
        btn.action = {
            val mockWs = TestHelper.makeShellWorkspace(200,200, renderEngine = master.renderEngine, strokeProvider = master.strokeDrawerProvider)
            master.workspaceSet.addWorkspace(mockWs)

            val x = mockWs.groupTree.addNewSimpleLayer(null, "Layer1", DYNAMIC)
                    //mockWs.groupTree.addNewSimpleLayer(null, "Layer1", FLAT, 150,150)
//            x.x = 25
//            x.y = 25
            //val gc = (x.layers.activeData.handle.medium as FlatMedium).image.graphics
            //gc.fillRect( 25, 25, 75,75)
        }

        val btn2 = Hybrid.ui.Button("Goto First Workspace")
        btn2.action = {
            master.workspaceSet.currentWorkspace = master.workspaceSet.workspaces.firstOrNull()
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