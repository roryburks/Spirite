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
        val rects = drawFrames
                .map { frame ->
                    val corners = frame.flatMap { listOf(
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f, it.handle.y.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f + it.handle.width.f, it.handle.y.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f, it.handle.y.f + it.handle.height.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f + it.handle.width.f, it.handle.y.f + it.handle.height.f))
                            ) }

                    val x1 = corners.minBy { it.xf }?.xf?.floor ?: 0
                    val y1 =corners.minBy { it.yf }?.yf?.floor ?: 0
                    val x2 = corners.maxBy { it.xf }?.xf?.ceil ?: 0
                    val y2 =corners.maxBy { it.yf }?.yf?.ceil ?: 0
                    RectI(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2))
                }
        val x1 = rects.minBy { it.x1i }?.x1i ?: 0
        val y1 =rects.minBy { it.y1i }?.y1i ?: 0
        val x2 = rects.maxBy { it.x2i }?.x2i ?: 0
        val y2 =rects.maxBy { it.y2i }?.y2i ?: 0
        val drawRect = RectI(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2))

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