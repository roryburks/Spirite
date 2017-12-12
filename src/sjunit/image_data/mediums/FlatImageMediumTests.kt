package sjunit.image_data.mediums


import sjunit.TestWrapper
import spirite.base.image_data.mediums.IMedium.InternalImageTypes.NORMAL
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.PenTraits.PenState
import spirite.base.pen.StrokeParams
import spirite.base.util.Colors
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.hybrid.HybridHelper
import org.junit.Test as test

class FlatImageMediumTests
{
    @test fun TestSimpleCreation () {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val node = ws.addNewSimpleLayer(null, 120, 100, "Flat", 0, NORMAL)

            ws.selectedNode = node
            val building = ws.buildActiveData()
            building.doOnBuiltData {
                assert( it.compositeWidth == it.sourceWidth && it.compositeWidth == 120)
                assert( it.compositeHeight == it.sourceHeight && it.compositeHeight == 100)
            }
        }
    }

    @test fun TestDrawWithOffset () {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val node = ws.addNewSimpleLayer(null, 120, 100, "Flat", 0, NORMAL)
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
                it.renderEngine.renderWorkspace(ws, img1.graphics, Transform.IdentityMatrix)

                for (i in 50 until 75) {
                    val rgb = img1.getRGB(i, i)
                    assert( rgb == Colors.GREEN)
                }
                for( i in (0 until 45) union (80 until 100)) {
                    val rgb = img1.getRGB(i, i)
                    assert( rgb != Colors.GREEN)
                    if( i >= 80)
                        assert( rgb == Colors.RED)
                }

                // Finally, finish the stroke and verify that it anchors to the image as expected
                ws.setActiveStrokeEngine( null)
                drawer.endStroke()

                val img2 = HybridHelper.createImage(125, 125)
                it.renderEngine.renderWorkspace(ws, img2.graphics, Transform.IdentityMatrix)

                for (i in 50 until 75) {
                    val rgb = img2.getRGB(i, i)
                    assert( rgb == Colors.GREEN)
                }
                for( i in (0 until 45) union (80 until 100)) {
                    val rgb = img2.getRGB(i, i)
                    assert( rgb != Colors.GREEN)
                }

                // takes ~12 seconds, but should pass
                //verifyRawImagesAreEqual(img1, img2)
            }
        }

    }
}