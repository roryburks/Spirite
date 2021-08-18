package spirite.base.imageData.animation

import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.ceil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import rb.vectrix.shapes.RectI
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import kotlin.math.abs

object AnimationUtil {
    fun getAnimationBoundaries( animation: FixedFrameAnimation) : RectI {

        val drawFrames = (animation.start until animation.end).asSequence()
                .map { animation .getDrawList(it.f).asSequence() }
        val rects = drawFrames
                .map { frame ->
                    val corners = frame.flatMap { sequenceOf(
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f, it.handle.y.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f + it.handle.width.f, it.handle.y.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f, it.handle.y.f + it.handle.height.f)),
                            it.renderRubric.transform.apply(Vec2f(it.handle.x.f + it.handle.width.f, it.handle.y.f + it.handle.height.f))
                    ) }

                    val x1 = corners.minBy { it.xf }?.xf?.floor ?: 0
                    val y1 =corners.minBy { it.yf }?.yf?.floor ?: 0
                    val x2 = corners.maxBy { it.xf }?.xf?.ceil ?: 0
                    val y2 =corners.maxBy { it.yf }?.yf?.ceil ?: 0
                    RectI(Math.min(x1, x2), Math.min(y1, y2), abs(x1 - x2), abs(y1 - y2))
                }
        val x1 = rects.minBy { it.x1i }?.x1i ?: 0
        val y1 =rects.minBy { it.y1i }?.y1i ?: 0
        val x2 = rects.maxBy { it.x2i }?.x2i ?: 0
        val y2 =rects.maxBy { it.y2i }?.y2i ?: 0

        return RectI(Math.min(x1, x2), Math.min(y1, y2), abs(x1 - x2), abs(y1 - y2))
    }
}