package spirite.base.file

import rb.extendo.extensions.then
import rb.glow.gl.GLImage
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.ceil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import rb.vectrix.shapes.RectI
import rbJvm.glow.awt.ImageBI
import rbJvm.util.GifSequenceWriter
import spirite.base.imageData.animation.AnimationUtil
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.layers.SimpleLayer
import spirite.hybrid.Hybrid
import java.awt.image.BufferedImage
import java.io.File
import java.util.ArrayList
import javax.imageio.stream.FileImageOutputStream


fun exportGroupGif(group: GroupNode, file: File, fps: Float) {
    val biList = ArrayList<BufferedImage>()


    for (node in group.children) {
        if (node is LayerNode) {
            val l = node.layer
            if (l is SimpleLayer) {
//                biList.add((l.getActiveData().handle.deepAccess() as ImageBI).img)
            }
        }
    }

    val ios = FileImageOutputStream(file)

    val gsw = GifSequenceWriter(
            ios,
            biList[0].type,
            (1000 / fps).toInt(),
            true)

    for (bi in biList) {
        gsw.writeToSequence(bi)
    }

    gsw.close()
    ios.close()
}

object ExportToGif {
    fun exportAnim( animation: FixedFrameAnimation, file: File, fps: Float) {

        val drawFrames = (animation.start until animation.end)
                .map { animation.getDrawList(it.f) }
        val drawRect = AnimationUtil.getAnimationBoundaries(animation)

        val biList = drawFrames
                .map { frame ->
                    val gl = GLImage(drawRect.wi, drawRect.hi, Hybrid.gle)
                    val gc = gl.graphics
                    val trans = ImmutableTransformF.Translation(-drawRect.x1i.f, -drawRect.y1i.f)

                    frame
                            .map { it.stack(trans) }
                            .forEach { it.draw(gc) }

                    gl
                }
                .map { Hybrid.imageConverter.convert(it, ImageBI::class) as ImageBI }
                .map { it.bi }

        val ios = FileImageOutputStream(file)

        val gsw = GifSequenceWriter(
                ios,
                biList[0].type,
                (1000 / fps).toInt(),
                true)

        for (bi in biList) {
            gsw.writeToSequence(bi)
        }

        gsw.close()
        ios.close()
    }

}