package spirite.base.imageData.drawer

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.Composite.DST_IN
import spirite.base.graphics.RawImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.util.Color
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2

class LiftedImageDrawer(val workspace: IImageWorkspace) : IImageDrawer,
        IClearModule,
        IInvertModule,
        IColorChangeModule,
        IFillModule,
        ITransformModule
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
            rawImage.drawer.fill(p.x.floor, p.y.floor, color)
        }

        return true
    }

    override fun transform(trans: Transform) {
        val mask = workspace.selectionEngine.selection ?: return
        val rect = when {
            mask.transform == null -> Rect(mask.width, mask.height)
            else -> MathUtil.circumscribeTrans(Rect(mask.width, mask.height), mask.transform)
        }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans = Transform.TranslationMatrix(cx,cy) * trans * Transform.TranslationMatrix(-cx,-cy)

        workspace.undoEngine.doAsAggregateAction("Lift and Transform") {
            workspace.selectionEngine.transformSelection(effectiveTrans, true)
            workspace.selectionEngine.bakeTranslationIntoLifted()
        }
    }

    override fun startManipulatingTransform(): Rect? {
        workspace.selectionEngine.proposingTransform = workspace.toolset.Reshape.transform

        val selected = workspace.selectionEngine.selection ?: return null
        val lifted = workspace.selectionEngine.liftedData ?: return null

        return when(selected.transform) {
            null -> Rect(lifted.width, lifted.height)
            else -> MathUtil.circumscribeTrans(Rect(lifted.width,lifted.height), selected.transform)
        }
    }

    override fun stepManipulatingTransform() {
        val lifted = workspace.selectionEngine.liftedData ?: return
        val cx = lifted.width / 2f
        val cy = lifted.height / 2f

        workspace.selectionEngine.proposingTransform = Transform.TranslationMatrix(cx,cy) *
                workspace.toolset.Reshape.transform * Transform.TranslationMatrix(-cx,-cy)
    }

    override fun endManipulatingTransform() {
        workspace.selectionEngine.proposingTransform = null
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