package sjunit.base.imageData.undo

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import rb.glow.gl.GLImage
import rbJvm.vectrix.SetupVectrixForJvm
import spirite.sguiHybrid.EngineLaunchpoint
import spirite.sguiHybrid.Hybrid
import sjunit.testHelpers.runTest
import spirite.base.brains.MasterControl
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumRepository
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.undo.ImageContext
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeParams
import spirite.pc.setupSwGuiStuff
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tags(Tag("Integration"))
class ImageContextTests {
    @Test fun basic(){
        runTest {
            EngineLaunchpoint.gle

            val master = MasterControl()
            val ws = master.createWorkspace(100,100).also { master.workspaceSet.addWorkspace(it) }
            val medium = DynamicMedium(ws)
            val handle = ws.mediumRepository.addMedium(medium)
            val layer = SimpleLayer(handle)
            ws.groupTree.importLayer(null, "thing", layer, true)
            val drawer = medium.getImageDrawer(ArrangedMediumData(handle, 0f, 0f))
            drawer.startStroke(StrokeParams(), PenState(0f, 0f, 1f))
            drawer.stepStroke(PenState(100f, 100f, 1f))
            drawer.endStroke()

            val img1 = ws.renderEngine.renderWorkspace(ws)
            assertTrue(img1.getColor(50,50).alpha > 0.9)

            ws.undoEngine.undo()
            for (x in 0..99 step 5){
                for(y in 0..99 step 5){
                    assertEquals(0, medium.getColor(x, y).argb32)
                }
            }

            val img2 = ws.renderEngine.renderWorkspace(ws)
            for (x in 0..99 step 5){
                for(y in 0..99 step 5){
                    assertEquals(0, img2.getColor(x, y).argb32)
                }
            }

            ws.undoEngine.redo()
            val img3 = ws.renderEngine.renderWorkspace(ws)
            assertTrue(img3.getColor(50,50).alpha > 0.9)
        }
    }

    @Test fun basicCreateContextTest(){
        SetupVectrixForJvm()
        setupSwGuiStuff()

        SwingUtilities.invokeAndWait {
            val imageWorkspace = mockk<MImageWorkspace>(relaxed = true)
            every { imageWorkspace.width } returns 100
            every { imageWorkspace.height } returns 100
            val repo = MediumRepository(imageWorkspace)
            val medium = FlatMedium(GLImage(100, 100, Hybrid.gle, true), repo)
            val handle = repo.addMedium(medium)
            val imageContext = ImageContext(handle, imageWorkspace)

            val drawer = medium.getImageDrawer(ArrangedMediumData(handle, 0f, 0f))
            drawer.startStroke(StrokeParams(), PenState(0f, 0f, 1f))
            drawer.stepStroke(PenState(100f, 100f, 1f))
            drawer.endStroke()
            println(medium.getColor(50, 50).argb32)
        }
    }
}