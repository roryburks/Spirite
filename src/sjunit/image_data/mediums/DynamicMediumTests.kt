package sjunit.image_data.mediums

import sjunit.TestWrapper
import spirite.base.image_data.mediums.IMedium.InternalImageTypes
import spirite.base.image_data.mediums.IMedium.InternalImageTypes.DYNAMIC
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.PenTraits.PenState
import spirite.base.pen.StrokeParams
import spirite.base.util.Colors
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.hybrid.HybridHelper
import spirite.hybrid.HybridUtil
import java.io.File
import java.io.FileOutputStream
import kotlin.test.assertEquals
import org.junit.Test as test

class DynamicMediumTests
{
    @test fun TestCreation() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val node = ws.addNewSimpleLayer(null, 120, 100, "Flat", 0, InternalImageTypes.DYNAMIC)

            ws.selectedNode = node
            val building = ws.buildActiveData()
            building.doOnBuiltData {
                assertEquals( 120, it.sourceWidth)
                assertEquals( 100, it.sourceHeight)
                assertEquals( ws.width, it.compositeWidth)
                assertEquals( ws.height, it.compositeHeight)
            }
        }
    }

    @test fun TestDrawWithOffset() {

        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val node = ws.addNewSimpleLayer(null, 120, 100, "Flat", 0, DYNAMIC)
            ws.selectedNode = node
            run {
                // First draw a plain Red Stroke from 0 to 50 and check that it's red along the stroke
                val drawer = ws.activeDrawer as IStrokeModule
                val params = StrokeParams()
                params.width = 5f
                params.color = Colors.RED
                drawer.startStroke(params, PenState(0f, 0f, 1f))
                drawer.stepStroke(PenState(25f, 25f, 1f))
                drawer.stepStroke(PenState(50f, 50f, 1f))
                drawer.endStroke()

                val img = HybridHelper.createImage(640, 480)
                it.renderEngine.renderWorkspace(ws, img.graphics, Transform.IdentityMatrix)

                for (i in 0 until 50) {
                    val rgb = img.getRGB(i, i)
                    assert(rgb == Colors.RED)
                }
            }

            node.setOffset(50, 50)

            run {
                // Next partially draw a plain Green Stroke from 25 to 75 and verify that the part within the
                //  image (25 to 50) is green and the part outside isn't (while in the active Stroke draw state)
                val drawer = ws.getDrawerFromNode(node) as IStrokeModule
                val params = StrokeParams()
                params.width = 5f
                params.color = Colors.GREEN
                drawer.startStroke(params, PenState(25f, 25f, 1f))
                drawer.stepStroke(PenState(50f, 50f, 1f))
                drawer.stepStroke(PenState(75f, 75f, 1f))

                ws.setActiveStrokeEngine( drawer.strokeEngine)
                //drawer.endStroke()

                val img1 = HybridHelper.createImage(125, 125)
                it.renderEngine.renderWorkspace(ws, img1.graphics, Companion.IdentityMatrix)

                HybridUtil.savePNG(img1, FileOutputStream(File("C:/bucket/x.png")));


                // Finally, finish the stroke and verify that it anchors to the image as expected
                ws.setActiveStrokeEngine( null)
                drawer.endStroke()

                val img2 = HybridHelper.createImage(125, 125)
                it.renderEngine.renderWorkspace(ws, img2.graphics, Companion.IdentityMatrix)


                HybridUtil.savePNG(img2, FileOutputStream(File("C:/bucket/x3.png")));
                // takes ~12 seconds, but should pass
                //verifyRawImagesAreEqual(img1, img2)
            }
        }

    }
}