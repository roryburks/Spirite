package sjunit.base.imageData.undo

import io.mockk.every
import io.mockk.mockk
import rb.glow.color.Colors
import rb.glow.gl.GLImage
import rbJvm.vectrix.SetupVectrixForJvm
import spirite.base.brains.MasterControl
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumRepository
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.undo.ImageContext
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeParams
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.Hybrid
import spirite.pc.setupSwGuiStuff
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import org.junit.Test as test

class ImageContextTests {
    @test fun basic(){
        SetupVectrixForJvm()
        setupSwGuiStuff()


        SwingUtilities.invokeAndWait {
            EngineLaunchpoint.gle

            val master = MasterControl()
            val ws = master.createWorkspace(100,100)
            val medium = DynamicMedium(ws)
            val handle = ws.mediumRepository.addMedium(medium)
            val drawer = medium.getImageDrawer(ArrangedMediumData(handle, 0f, 0f))
            drawer.startStroke(StrokeParams(), PenState(0f, 0f, 1f))
            drawer.stepStroke(PenState(100f, 100f, 1f))
            drawer.endStroke()
            println("zzz:"+medium.getColor(50, 50).argb32)

            ws.undoEngine.undo()
            for (x in 0..99){
                for(y in 0..99){
                    assertEquals(0, medium.getColor(x, y).argb32)
                }
            }
        }
    }

    @test fun basicCreateContextTest(){
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