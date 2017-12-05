package sjunit.image_data.animations

import sjunit.TestWrapper
import spirite.base.image_data.animations.ffa.FFAFrameStructure
import spirite.base.image_data.animations.ffa.FFALayerGroupLinked
import spirite.base.image_data.animations.ffa.FixedFrameAnimation
import spirite.base.image_data.mediums.IMedium
import java.io.File
import org.junit.Test as test

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

    @test fun CreateAndUpdateWithSublinks() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val root = ws.addGroupNode(null, "AnimGroup")
            val frame1 = ws.addNewSimpleLayer(root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
            val inner = ws.addGroupNode( root, "InnerGroup")
            val frame2 = ws.addNewSimpleLayer(inner, 10, 10, "frame2", 0, IMedium.InternalImageTypes.NORMAL)
            var frame3 = ws.addNewSimpleLayer(root, 10, 10, "frame3", 0, IMedium.InternalImageTypes.NORMAL)


            val ffa = FixedFrameAnimation("Animation", ws)
            ffa.addLinkedLayer(root, true, null)
            ws.animationManager.addAnimation(ffa)

            assert( ffa.end == 4)

            val frame4 = ws.addNewSimpleLayer(frame2, 10, 10, "frame2b", 0, IMedium.InternalImageTypes.NORMAL)
            assert( ffa.end == 5)

            val frames = ffa.layers[0].frames
            assert( frames[0].marker == FFAFrameStructure.Marker.FRAME )
            assert( frames[1].marker == FFAFrameStructure.Marker.START_LOCAL_LOOP )
            assert( frames[2].marker == FFAFrameStructure.Marker.FRAME )
            assert( frames[3].marker == FFAFrameStructure.Marker.FRAME )
            assert( frames[4].marker == FFAFrameStructure.Marker.END_LOCAL_LOOP )
            assert( frames[5].marker == FFAFrameStructure.Marker.FRAME )

        }
    }
    @test fun AddAndUndoGapChange() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val root = ws.addGroupNode(null, "AnimGroup")
            ws.addNewSimpleLayer(root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
            ws.addNewSimpleLayer(root, 10, 10, "frame2", 0, IMedium.InternalImageTypes.NORMAL)
            ws.addNewSimpleLayer(root, 10, 10, "frame3", 0, IMedium.InternalImageTypes.NORMAL)


            val ffa = FixedFrameAnimation("Animation", ws)
            ffa.addLinkedLayer(root, true, null)
            ws.animationManager.addAnimation(ffa)

            ffa.layers[0].frames[1].gapBefore = 10;

            assert( ffa.end == 14)

            ws.undoEngine.undo()
            assert( ffa.end == 4)

        }
    }

    @test fun SaveAndLoadAnim() {
        TestWrapper.performTest {
            val ws = it.currentWorkspace
            val root = ws.addGroupNode(null, "AnimGroup")
            val frame1 = ws.addNewSimpleLayer(root, 10, 10, "frame1", 0, IMedium.InternalImageTypes.NORMAL)
            val inner = ws.addGroupNode(root, "InnerGroup")
            val frame2 = ws.addNewSimpleLayer(inner, 10, 10, "frame2", 0, IMedium.InternalImageTypes.NORMAL)
            val frame3 = ws.addNewSimpleLayer(root, 10, 10, "frame3", 0, IMedium.InternalImageTypes.NORMAL)
            val frame4 = ws.addNewSimpleLayer(frame2, 10, 10, "frame2b", 0, IMedium.InternalImageTypes.NORMAL)


            val ffa = FixedFrameAnimation("Animation", ws)
            ffa.addLinkedLayer(root, true, null)
            ws.animationManager.addAnimation(ffa)

            val frames = ffa.layers[0].frames
            frames[0].gapBefore = 1
            frames[2].gapAfter = 2

            val tempFile = File.createTempFile("ffa_sala","sif")
            it.saveWorkspace(ws, tempFile)

            val ws2 = it.loadEngine.loadWorkspace(tempFile)

            assert(ws2.animationManager.animations.size != 0)
            val frames2 = (ws.animationManager.animations[0] as FixedFrameAnimation).layers[0].frames
            assert(frames2.size == 6)
            assert(frames2[0].gapBefore == 1)
            assert(frames2[2].gapAfter == 2)
        }
    }
}