package spirite.base.imageData.animation

import rb.glow.GraphicsContext
import rb.glow.LineAttributes
import rb.glow.Colors
import rb.vectrix.mathUtil.MathUtil
import spirite.base.imageData.IImageWorkspace

class FakeAnimation(workspace: IImageWorkspace) : Animation("Fake", workspace,
        AnimationState(11.1f, 3, 5.53f))
{

    override val startFrame: Float
        get() = 0f
    override val endFrame: Float
        get() = 100f

    override fun drawFrame(gc: GraphicsContext, t: Float) {
        val t = MathUtil.cycle(0f, 100f, t)

        gc.lineAttributes = LineAttributes(3f)
        gc.color = Colors.RED
        gc.drawLine(100f - t, 0f, t, 100f)
    }

    override fun dupe()= FakeAnimation(workspace)
}