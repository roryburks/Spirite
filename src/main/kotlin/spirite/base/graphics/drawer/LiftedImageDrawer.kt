package spirite.base.graphics.drawer

import rb.glow.Color
import rb.glow.Composite.DST_IN
import rb.glow.img.RawImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.drawer.IImageDrawer.*
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import spirite.specialRendering.SpecialDrawerFactory

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
        doToUnderlying { SpecialDrawerFactory.makeSpecialDrawer(it.graphics).invert() }
    }

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        doToUnderlying { SpecialDrawerFactory.makeSpecialDrawer(it.graphics).changeColor(from, to, mode) }
    }
    override fun fill(x: Int, y: Int, color: Color): Boolean {
        doToUnderlyingWithTrans { rawImage, transform ->
            val p = transform?.invert()?.apply(Vec2f(x.f,y.f)) ?: Vec2f(x.f,y.f)
            SpecialDrawerFactory.makeSpecialDrawer(rawImage.graphics).fill(p.xf.floor, p.yf.floor, color)
        }

        return true
    }

    override fun transform(trans: ITransformF, centered : Boolean) {
        val mask = workspace.selectionEngine.selection ?: return
        val rect = when {
            mask.transform == null -> Rect(mask.width, mask.height)
            else -> RectangleUtil.circumscribeTrans(Rect(mask.width, mask.height), mask.transform)
        }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans =
                if( centered) ImmutableTransformF.Translation(cx,cy) * trans * ImmutableTransformF.Translation(-cx,-cy)
                else trans

        workspace.undoEngine.doAsAggregateAction("Lift and ITransformF") {
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
            else -> RectangleUtil.circumscribeTrans(Rect(lifted.width,lifted.height), selected.transform)
        }
    }

    override fun stepManipulatingTransform() {
        val lifted = workspace.selectionEngine.liftedData ?: return
        val cx = lifted.width / 2f
        val cy = lifted.height / 2f

        workspace.selectionEngine.proposingTransform = ImmutableTransformF.Translation(cx,cy) *
                workspace.toolset.Reshape.transform * ImmutableTransformF.Translation(-cx,-cy)
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
                gc.renderImage(selection.mask, 0.0, 0.0)
            }
        })
        workspace.selectionEngine.setSelectionWithLifted(selection,newLifted)
    }

    // Replaces the Lifted Image with one that is (presumably) modified by the lambda
    private inline fun doToUnderlyingWithTrans(lambda: (RawImage, ITransformF?)->Any?) {
        val selection = workspace.selectionEngine.selection ?: return
        val lifted = workspace.selectionEngine.liftedData as? LiftedImageData ?: return

        val newLifted = LiftedImageData(lifted.image.deepCopy().apply {
            lambda.invoke(this, selection.transform)
            this.graphics.also { gc ->
                gc.composite = DST_IN
                gc.renderImage(selection.mask, 0.0, 0.0)
            }
        })
        workspace.selectionEngine.setSelectionWithLifted(selection,newLifted)
    }
}