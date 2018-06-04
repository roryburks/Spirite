package spirite.base.imageData.animation

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.util.Colors
import spirite.base.util.MathUtil

class FakeAnimation(workspace: IImageWorkspace) : Animation("Fake", workspace,
        AnimationState(11.1f, 3, 5.53f))
{
    override val startFrame: Float
        get() = 0f
    override val endFrame: Float
        get() = 100f

    override fun drawFrame(gc: GraphicsContext, t: Float) {
        val t = MathUtil.cycle(0f, 100f, t)

        gc.color = Colors.RED
        gc.drawLine(100f - t, 0f, t, 100f)
    }

}