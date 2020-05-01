package sjunit.base.brains

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rb.owl.observer
import spirite.base.brains.CentralObservatory
import spirite.base.brains.WorkspaceSet
import spirite.base.imageData.IImageObservatory
import spirite.base.imageData.ImageObservatory
import spirite.base.imageData.MImageWorkspace
import kotlin.test.assertEquals

open class CentralObservatoryTests {
    val wsSet = WorkspaceSet()
    val service = CentralObservatory(wsSet)

    class OmniImageObserverTests : CentralObservatoryTests() {
        @Test fun tryTest(){
            assert(true)
        }

        @Test fun TracksSingle() {
            // Arrange
            var numImgChangedCalled = 0
            service.omniImageObserver.addObserver(object : IImageObservatory.ImageObserver {
                override fun imageChanged(evt: IImageObservatory.ImageChangeEvent) {
                    ++numImgChangedCalled
                }
            }.observer())
            val imageObserver = ImageObservatory()
            val mockWs = mockk<MImageWorkspace>(relaxed = true)
            every { mockWs.imageObservatory }.returns(imageObserver)
            wsSet.addWorkspace(mockWs)

            // Act
            imageObserver.triggerRefresh(IImageObservatory.ImageChangeEvent(emptySet(), emptySet(), mockWs))

            // Assert
            assertEquals(1, numImgChangedCalled)
        }


        @Test fun TracksMulti() {
            // Arrange
            var numImgChangedCalled = 0
            service.omniImageObserver.addObserver(object : IImageObservatory.ImageObserver {
                override fun imageChanged(evt: IImageObservatory.ImageChangeEvent) {
                    ++numImgChangedCalled
                }
            }.observer())

            val imageObserver1 = ImageObservatory()
            val mockWs1 = mockk<MImageWorkspace>(relaxed = true)
            every { mockWs1.imageObservatory }.returns(imageObserver1)
            wsSet.addWorkspace(mockWs1)

            val imageObserver2 = ImageObservatory()
            val mockWs2 = mockk<MImageWorkspace>(relaxed = true)
            every { mockWs2.imageObservatory }.returns(imageObserver2)
            wsSet.addWorkspace(mockWs2)

            // Act
            imageObserver1.triggerRefresh(IImageObservatory.ImageChangeEvent(emptySet(), emptySet(), mockWs1))
            imageObserver2.triggerRefresh(IImageObservatory.ImageChangeEvent(emptySet(), emptySet(), mockWs2))

            // Assert
            assertEquals(2, numImgChangedCalled)
        }


        @Test fun TracksMulti_Removed() {
            // Arrange
            var numImgChangedCalled = 0
            service.omniImageObserver.addObserver(object : IImageObservatory.ImageObserver {
                override fun imageChanged(evt: IImageObservatory.ImageChangeEvent) {
                    ++numImgChangedCalled
                }
            }.observer())

            val imageObserver1 = ImageObservatory()
            val mockWs1 = mockk<MImageWorkspace>(relaxed = true)
            every { mockWs1.imageObservatory }.returns(imageObserver1)
            wsSet.addWorkspace(mockWs1)

            val imageObserver2 = ImageObservatory()
            val mockWs2 = mockk<MImageWorkspace>(relaxed = true)
            every { mockWs2.imageObservatory }.returns(imageObserver2)
            wsSet.addWorkspace(mockWs2)

            wsSet.removeWorkspace(mockWs2)

            // Act
            imageObserver1.triggerRefresh(IImageObservatory.ImageChangeEvent(emptySet(), emptySet(), mockWs1))
            imageObserver2.triggerRefresh(IImageObservatory.ImageChangeEvent(emptySet(), emptySet(), mockWs2))

            // Assert
            assertEquals(1, numImgChangedCalled)
        }
    }

}