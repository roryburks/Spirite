package spirite.base.imageData.drawer

import rb.glow.RawImage
import rb.glow.color.Color
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.brains.toolset.ColorChangeMode
import rb.glow.Composite.DST_OUT
import rb.glow.Composite.SRC
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.selection.ILiftedData
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.imageData.selection.Selection
import spirite.base.imageData.undo.ImageAction
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import spirite.hybrid.Hybrid
import spirite.specialRendering.SpecialDrawerFactory

class DefaultImageDrawer(
        val arranged: ArrangedMediumData)
    :IImageDrawer,
        IStrokeModule by DefaultImageDrawer.StrokeModule(arranged),
        IInvertModule,
        IClearModule,
        ILiftSelectionModule,
        IAnchorLiftModule,
        IColorChangeModule,
        IFillModule,
        ITransformModule
{
    val workspace : IImageWorkspace get() = arranged.handle.workspace
    val mask get() = workspace.selectionEngine.selection

    class StrokeModule(val arranged: ArrangedMediumData) : IStrokeModule {
        val workspace : IImageWorkspace get() = arranged.handle.workspace
        val mask get() = workspace.selectionEngine.selection

        private var strokeBuilder : StrokeBuilder? = null

        override fun canDoStroke(method: Method) = true

        override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
            val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
            val sb = StrokeBuilder(strokeDrawer, params, arranged)
            strokeBuilder = sb

            workspace.compositor.compositeSource = CompositeSource(arranged) {strokeDrawer.draw(it)}

            if(sb.start(ps))
                arranged.handle.refresh()

            return true
        }

        override fun stepStroke(ps: PenState) {
            if(strokeBuilder?.step(ps) == true)
                workspace.compositor.triggerCompositeChanged()
        }

        override fun endStroke() {
            val sb = strokeBuilder ?: return

            // NOTE: Storing the bakedDrawPoints rather than the baseStates, this means two things:
            //  1: Far more drawPoints are stored than is necessary, but don't need to be recalculated every time (both minimal)
            //  2: You need to use rawAccessComposite rather than drawToComposite as the drawPoints are already transformed
            val bakedDrawPoints = sb.currentPoints
            val params = sb.params
            workspace.undoEngine.performAndStore( object : ImageAction(arranged) {
                override val description: String get() = "Pen Stroke"

                override fun performImageAction(built: BuiltMediumData) {
                    val drawer = built.arranged.handle.workspace.strokeProvider.getStrokeDrawer(params)
                    built.rawAccessComposite {
                        drawer.batchDraw(it.graphics, bakedDrawPoints, params, built.width, built.height)
                    }
                }
            })

            sb.end()
            workspace.compositor.compositeSource = null
        }
    }


    // region IInvertModule
    override fun invert() {
        workspace.undoEngine.performMaskedImageAction("Invert", arranged, mask) { built, mask ->
            when( mask) {
                null -> built.rawAccessComposite {  SpecialDrawerFactory.makeSpecialDrawer(it.graphics).invert() }
                else -> built.rawAccessComposite { raw ->
                    mask.doMasked(raw, built.tWorkspaceToComposite) {SpecialDrawerFactory.makeSpecialDrawer(it.graphics).invert()} }
            }
        }
    }
    // endregion


    // region ISelectionModule
    override fun clear() {
        workspace.undoEngine.performMaskedImageAction("Clear", arranged, mask) { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { it.graphics.clear() }
                else -> built.rawAccessComposite { raw -> mask.doMasked(raw, built.tWorkspaceToComposite) { it.graphics.clear() } }
            }
        }
    }
    // endregion

    // region ILiftSelectionModule


    override fun liftSelection(selection: Selection,  clearLifted: Boolean): ILiftedData {
        var lifted : RawImage? = null   // ugly "kind of stateful but not really" solution, but meh
        val tbuilt = arranged.built
        tbuilt.rawAccessComposite { lifted = selection.lift(it, tbuilt.tWorkspaceToComposite) }

        if( clearLifted) {
            workspace.undoEngine.performMaskedImageAction("lift-inner", arranged, null) { built, mask ->
                built.rawAccessComposite {
                    it.graphics.apply {
                        val tSelToImage = (built.tWorkspaceToComposite) * (selection.transform
                                ?: ImmutableTransformF.Identity)
                        transform = tSelToImage
                        composite = DST_OUT
                        renderImage(selection.mask, 0, 0)
                    }
                }
            }
        }

        return LiftedImageData(lifted ?: Hybrid.imageCreator.createImage(1,1))
    }
    // endregion

    // region IAnchorLiftModule

    override fun acceptsLifted(lifted: ILiftedData) = true

    override fun anchorLifted(lifted: ILiftedData, trans: ITransformF?) {
        workspace.undoEngine.performMaskedImageAction("Anchor Lifted", arranged, null) { built, mask ->
            built.drawOnComposite { gc->
                if(trans != null)
                    gc.preTransform(trans)
                lifted.draw(gc)
            }
        }
    }

    // endregion

    // region IColorChangeModule

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        workspace.undoEngine.performMaskedImageAction("ChangeColor", arranged, mask) { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { SpecialDrawerFactory.makeSpecialDrawer(it.graphics).changeColor(from, to, mode) }
                else -> built.rawAccessComposite {raw ->
                    mask.doMasked(raw, built.tWorkspaceToComposite) { SpecialDrawerFactory.makeSpecialDrawer(it.graphics).changeColor(from, to, mode) }
                }
            }
        }
    }

    // endregion

    // region IFillModule
    override fun fill(x: Int, y: Int, color: Color): Boolean {
        workspace.undoEngine.performMaskedImageAction("Fill Color", arranged, mask) { built, mask ->
            when( mask) {
                null -> built.rawAccessComposite {
                    val p = built.tWorkspaceToComposite.apply(Vec2f(x.f,y.f))
                    SpecialDrawerFactory.makeSpecialDrawer(it.graphics).fill(p.xf.floor, p.yf.floor, color)
                }
                else -> built.rawAccessComposite {raw ->
                    mask.doMaskedRequiringTransform(raw, built.tWorkspaceToComposite, color) { maskedRaw, tImageToFloating ->
                        val p =  tImageToFloating.apply(built.tWorkspaceToComposite.apply(Vec2f(x.f,y.f)))
                        SpecialDrawerFactory.makeSpecialDrawer(maskedRaw.graphics).fill(p.xf.floor,p.yf.floor, color)
                    }
                }
            }
        }
        return true
    }
    // endregion

    override fun transform(trans: ITransformF, centered : Boolean) {
        val mask = mask

        val rect = when {
            mask == null -> arranged.handle.run { Rect(x,y,width, height) }
            mask.transform == null -> Rect(mask.width, mask.height)
            else -> RectangleUtil.circumscribeTrans(Rect(mask.width, mask.height), mask.transform)
        }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans =
                if( centered) ImmutableTransformF.Translation(cx,cy) * trans * ImmutableTransformF.Translation(-cx,-cy)
                else trans

        doTransformStraight(effectiveTrans)
    }
    override fun flip(horizontal: Boolean) {
        val built = arranged.built
        val cx = built.width /2f
        val cy = built.height /2f

        doTransformStraight(when( horizontal) {
            true -> ImmutableTransformF.Translation(cx,cy) * ImmutableTransformF.Scale(-1f,1f) * ImmutableTransformF.Translation(-cx,-cy)
            false -> ImmutableTransformF.Translation(cx,cy) * ImmutableTransformF.Scale(1f, -1f) * ImmutableTransformF.Translation(-cx,-cy)
        })
    }

    private fun doTransformStraight( effectiveTrans: ITransformF) {
        if( mask == null) {
            workspace.undoEngine.performAndStore(object: ImageAction(arranged) {
                override val description: String get() = "ITransformF"

                override fun performImageAction(built: BuiltMediumData) {
                    built.rawAccessComposite {
                        val buffer = Hybrid.imageCreator.createImage(it.width, it.height)
                        val bgc = buffer.graphics
                        bgc.transform(effectiveTrans)
                        bgc.renderImage(it,0,0)

                        val igc = it.graphics
                        igc.composite = SRC
                        igc.renderImage(buffer,0,0)
                        buffer.flush()
                    }
                }
            })
        }
        else {
            workspace.undoEngine.doAsAggregateAction("Lift and ITransformF") {
                workspace.selectionEngine.transformSelection(effectiveTrans, true)
                workspace.selectionEngine.bakeTranslationIntoLifted()
            }
        }
    }



    override fun startManipulatingTransform(): Rect? {
        val tool = workspace.toolset.Reshape
        val selected = workspace.selectionEngine.selection
        val lifted = workspace.selectionEngine.liftedData

        if( selected != null && lifted == null) {
            workspace.undoEngine.doAsAggregateAction("Auto-lift") {
                val nLifted = liftSelection(selected)
                workspace.selectionEngine.setSelectionWithLifted(selected, nLifted)
            }
            return null
        }
        if( selected != null)
            workspace.selectionEngine.setSelection(null)

        workspace.compositor.compositeSource = CompositeSource(arranged, false ) { gc ->
            val medium = arranged.handle.medium
            val cx = medium.width / 2f + medium.x
            val cy = medium.height / 2f + medium.y

            val effectiveTrans = ImmutableTransformF.Translation(cx,cy) * tool.transform * ImmutableTransformF.Translation(-cx,-cy)

            gc.transform = effectiveTrans
            arranged.handle.medium.render(gc)
        }

        val m = arranged.handle
        return RectangleUtil.circumscribeTrans(Rect(m.x, m.y, m.width, m.height), arranged.tMediumToWorkspace)
    }

    override fun stepManipulatingTransform() {
        workspace.compositor.triggerCompositeChanged()
    }
    override fun endManipulatingTransform() {
        workspace.compositor.compositeSource = null
    }

    //endregion
}