package sjunit.base.imageData.undo

import io.mockk.every
import io.mockk.mockk
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumRepository
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.undo.ImageContext
import org.junit.Test as test

class ImageContextTests {
    interface  I {}
    @test fun basicCreateContextTest(){
        var simpleI = mockk<I>()
        val imageWorkspace = mockk<MImageWorkspace>(relaxed = true)
        every { imageWorkspace.width } returns 100
        every { imageWorkspace.height } returns 100
        val repo = MediumRepository(imageWorkspace)
        val medium = DynamicMedium(imageWorkspace)
        val handle = repo.addMedium(medium)
        val imageContext = ImageContext(handle, imageWorkspace)
    }
}