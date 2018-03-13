package demonstration

import io.mockk.mockk
import sjunit.TestConfig
import sjunit.TestHelper
import spirite.base.brains.WorkspaceSet
import spirite.base.graphics.IImageTracker
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.basic.SButton
import spirite.gui.advanced.ResizeContainerPanel
import spirite.gui.major.work.WorkSection
import java.awt.GridLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

val renderEngine = RenderEngine(mockk(relaxed = true), mockk(relaxed = true))
val workspaceSet = WorkspaceSet()
lateinit var ws: WorkSection

fun main( args: Array<String>) {
    SwingUtilities.invokeLater {
        val wsd = WorkSectionDemo()
        DemoLauncher.launch(wsd, 800, 600)
    }
}

class WorkSectionDemo : JFrame() {
    init {

        layout = GridLayout()

        //SwingUtilities.invokeLater {
        //}

        ws = WorkSection(workspaceSet)
        val resize = ResizeContainerPanel(ws, HORIZONATAL, 200)

        resize.minStretch = 100

        val btn = SButton("1")
        btn.action = {
            val mockWs = TestHelper.makeShellWorkspace(200,200, renderEngine = renderEngine)
            workspaceSet.addWorkspace(mockWs)

            val x = mockWs.groupTree.addNewSimpleLayer(null, "Layer1", FLAT, 100,100)
            val gc = (x.layer.activeData.handle.medium as FlatMedium).image.graphics
            gc.fillRect( 25, 25, 75,75)
        }

        val btn2 = SButton("2")
        btn2.action = {
            workspaceSet.currentWorkspace = workspaceSet.workspaces.first()
        }

        resize.addPanel(btn, 100,100,-999)
        resize.addPanel(btn2, 100,100,-999)
        resize.addPanel(SButton("3"), 100,100,999)
        resize.addPanel(SButton("4"), 100,100,999)

        add( resize.component)
    }
}