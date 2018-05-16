package spirite.base.imageData.drawer

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.GraphicsContext.Composite.DST_IN
import spirite.base.graphics.RawImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.util.Color
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2

class LiftedImageDrawer(val workspace: IImageWorkspace) : IImageDrawer,
        IClearModule,
        IInvertModule,
        IColorChangeModule,
        IFillModule
{
    override fun clear() {
        workspace.selectionEngine.clearLifted()
    }

    override fun invert() {
        doToUnderlying { it.drawer.invert() }
    }

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        doToUnderlying { it.drawer.changeColor(from, to, mode) }
    }
    override fun fill(x: Int, y: Int, color: Color): Boolean {
        doToUnderlyingWithTrans { rawImage, transform ->
            val p = transform?.invert()?.apply(Vec2(x.f,y.f)) ?: Vec2(x.f,y.f)
            println("${p.x.floor},${p.y.floor}")
            rawImage.drawer.fill(p.x.floor, p.y.floor, color)
        }

        return true
    }

    // Replaces the Lifted Image with one that is (presumably) modified by the lambda
    private inline fun doToUnderlying(lambda: (RawImage)->Any?) {
        val selection = workspace.selectionEngine.selection ?: return
        val lifted = workspace.selectionEngine.liftedData as? LiftedImageData ?: return
        val newLifted = LiftedImageData(lifted.image.deepCopy().apply {
            lambda.invoke(this)
            this.graphics.also { gc ->
                gc.composite = DST_IN
                gc.renderImage(selection.mask, 0, 0)
            }
        })
        workspace.selectionEngine.setSelectionWithLifted(selection,newLifted)
    }

    // Replaces the Lifted Image with one that is (presumably) modified by the lambda
    private inline fun doToUnderlyingWithTrans(lambda: (RawImage, Transform?)->Any?) {
        val selection = workspace.selectionEngine.selection ?: return
        val lifted = workspace.selectionEngine.liftedData as? LiftedImageData ?: return

        val newLifted = LiftedImageData(lifted.image.deepCopy().apply {
            lambda.invoke(this, selection.transform)
            this.graphics.also { gc ->
                gc.composite = DST_IN
                gc.renderImage(selection.mask, 0, 0)
            }
        })
        workspace.selectionEngine.setSelectionWithLifted(selection,newLifted)
    }
}