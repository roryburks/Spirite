package spirite.base.file

import rb.extendo.extensions.then
import rb.glow.color.ColorARGB32Normal
import rb.glow.color.Colors
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
                    gc.color = Colors.LIGHT_GRAY
                    gc.fillRect(0,0,drawRect.wi, drawRect.hi)
                    val trans = ImmutableTransformF.Translation(-drawRect.x1i.f, -drawRect.y1i.f)

                    frame
                            .map { it.stack(trans) }
                            .forEach { it.draw(gc) }

                    gl
                }
                .map { Hybrid.imageConverter.convert(it, ImageBI::class) as ImageBI }
                .map { it.bi }

        biList.forEach {
            (0 until it.width).forEach{x ->
                (0 until it.height).forEach { y ->
                    val argb  = it.getRGB(x,y)
                    val c = ColorARGB32Normal(argb)
                    val c2 = ColorARGB32Normal.FromComponents(
                            if( c.alpha == 0f) 0f else c.alpha / c.alpha,
                            if( c.alpha == 0f) 0f else c.red/ c.alpha,
                            if( c.alpha == 0f) 0f else c.green/ c.alpha,
                            if( c.alpha == 0f) 0f else c.blue/ c.alpha)
                    it.setRGB(x, y, c2.argb)
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

}