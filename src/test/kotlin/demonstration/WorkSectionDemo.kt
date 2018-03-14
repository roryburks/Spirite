package demonstration

import io.mockk.every
import io.mockk.mockk
import sjunit.TestHelper
import spirite.base.brains.MasterControl
import spirite.base.brains.WorkspaceSet
import spirite.base.brains.palette.PaletteManager
import spirite.base.brains.toolset.ToolsetManager
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.basic.SButton
import spirite.gui.advanced.ResizeContainerPanel
import spirite.gui.major.work.WorkSection
import spirite.hybrid.Hybrid
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

lateinit var master: MasterControl
lateinit var ws: WorkSection

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

        ws = WorkSection(master)
        val resize = ResizeContaigit nerPanel(ws, HORIZONATAL, 200)

        resize.minStretch = 100

        val btn = SButton("1")
        btn.action = {
            val mockWs = TestHelper.makeShellWorkspace(200,200, renderEngine = master.renderEngine, strokeProvider = master.strokeDrawerProvider)
            master.workspaceSet.addWorkspace(mockWs)

            val x = mockWs.groupTree.addNewSimpleLayer(null, "Layer1", FLAT, 150,150)
            x.x = 25
            x.y = 25
            val gc = (x.layer.activeData.handle.medium as FlatMedium).image.graphics
            gc.fillRect( 25, 25, 75,75)
        }

        val btn2 = SButton("2")
        btn2.action = {
            master.workspaceSet.currentWorkspace = master.workspaceSet.workspaces.first()
        }

        resize.addPanel(btn, 100,100,-999)
        resize.addPanel(btn2, 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(SButton("4"), 100,100,999)

        add( resize.component)
    }
}