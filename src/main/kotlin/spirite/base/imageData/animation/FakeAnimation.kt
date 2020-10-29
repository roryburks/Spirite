package spirite.base.imageData.animation

import rb.glow.*
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import spirite.base.imageData.IImageWorkspace

class FakeAnimation(workspace: IImageWorkspace) : Animation("Fake", workspace,
        AnimationState(11.1f, 3, 5.53f))
{

    override val startFrame: Float
        get() = 0f
    override val endFrame: Float
        get() = 100f

    override fun drawFrame(gc: IGraphicsContext, t: Float) {
        val t = MathUtil.cycle(0f, 100f, t)

        gc.lineAttributes = LineAttributes(3f)
        gc.color = Colors.RED
        gc.drawer.drawLine(100.0 - t, 0.0, t.d, 100.0)
    }

    override fun dupe()= FakeAnimation(workspace)
}