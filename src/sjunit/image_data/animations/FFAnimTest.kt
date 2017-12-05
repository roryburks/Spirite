package sjunit.image_data.animations

import kotlin.test.*
import org.junit.Test as test
import org.junit.Assert.*
import sjunit.TestWrapper
import spirite.base.image_data.animations.ffa.FFALayerGroupLinked
import spirite.base.image_data.animations.ffa.FixedFrameAnimation
import spirite.base.image_data.mediums.IMedium

class FixedFrameAnimationTests {
    @test fun CreateSimpleFourFrameAnimation() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace

            val root = ws.addGroupNode(null, "AnimGroup")!!
            ws.addNewSimpleLayer( root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
            ws.addNewSimpleLayer( root, 10, 10, "frame2", 0, IMedium.InternalImageTypes.NORMAL)
            ws.addNewSimpleLayer( root, 10, 10, "frame3", 0, IMedium.InternalImageTypes.NORMAL)
            ws.addNewSimpleLayer( root, 10, 10, "frame4", 0, IMedium.InternalImageTypes.NORMAL)

            val ffa = FixedFrameAnimation("Animation", ws)
            ffa.addLinkedLayer(root, false, null)
            assert( ffa.end == 5)

            ws.animationManager.addAnimation(ffa)
            assert( ws.animationManager.animations[0] == ffa)
        }
    }

    @test fun CreateAndUpdateSimpleAnimation() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val root = ws.addGroupNode( null, "AnimGroup")
            ws.addNewSimpleLayer( root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
            val frame2 = ws.addNewSimpleLayer( root, 10, 10, "frame2", 0, IMedium.InternalImageTypes.NORMAL)


            val ffa = FixedFrameAnimation("Animation", ws)
            ffa.addLinkedLayer(root, false, null)
            ws.animationManager.addAnimation(ffa)

            ws.addNewSimpleLayer( frame2, 10, 10, "frame1a", 0, IMedium.InternalImageTypes.NORMAL)


            var frames = (ffa.layers[0] as FFALayerGroupLinked).frames
            assert( frames[0].node?.name == "frame2");
            assert( frames[1].node?.name == "frame1a");
            assert( frames[2].node?.name == "frame1");
        }
    }

    @test fun CreateWithSublinks() {

        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val root = ws.addGroupNode(null, "AnimGroup")
            ws.addNewSimpleLayer(root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
        }
    }
}