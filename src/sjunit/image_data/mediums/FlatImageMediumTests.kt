package sjunit.image_data.mediums


import sjunit.TestWrapper
import spirite.base.image_data.mediums.IMedium.InternalImageTypes.NORMAL
import org.junit.Test as test

class FlatImageMediumTests
{
    @test fun TestSimpleCreation () {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            ws.addNewSimpleLayer(null, 100, 100, "Flat", 0, NORMAL)


        }
    }
}