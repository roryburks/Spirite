package spirite.base.imageData.drawer

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.GraphicsContext.Composite.DST_OUT
import spirite.base.graphics.GraphicsContext.Composite.SRC
import spirite.base.graphics.RawImage
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
import spirite.base.util.Color
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.hybrid.Hybrid

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
            strokeBuilder = StrokeBuilder(strokeDrawer, params, arranged)

            workspace.compositor.compositeSource = CompositeSource(arranged) {strokeDrawer.draw(it)}

            if(strokeBuilder!!.start(ps))
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
            //  1: Far more points are stored than is necessary, but don't need to be recalculated every time (both minimal)
            //  2: You need to use rawAccessComposite rather than drawToComposite as the points are already transformed
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
        workspace.undoEngine.performMaskedImageAction("Invert", arranged, mask, { built, mask ->
            when( mask) {
                null -> built.rawAccessComposite { it.drawer.invert() }
                else -> built.rawAccessComposite { mask.doMasked(it, built.tWorkspaceToComposite) {it.drawer.invert()} }
            }
        })
    }
    // endregion


    // region ISelectionModule
    override fun clear() {
        workspace.undoEngine.performMaskedImageAction("Clear", arranged, mask, { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { it.graphics.clear() }
                else -> built.rawAccessComposite { mask.doMasked(it, built.tWorkspaceToComposite) { it.graphics.clear() } }
            }
        })
    }
    // endregion

    // region ILiftSelectionModule


    override fun liftSelection(selection: Selection,  clearLifted: Boolean): ILiftedData {
        var lifted : RawImage? = null   // ugly "kind of stateful but not really" solution, but meh
        val built = arranged.built
        built.rawAccessComposite { lifted = selection.lift(it, built.tWorkspaceToComposite) }

        if( clearLifted) {
            workspace.undoEngine.performMaskedImageAction("lift-inner", arranged, null, { built, mask ->
                built.rawAccessComposite {
                    it.graphics.apply {
                        val tSelToImage = (built.tWorkspaceToComposite) * (selection.transform
                                ?: Transform.IdentityMatrix)
                        transform = tSelToImage
                        composite = DST_OUT
                        renderImage(selection.mask, 0, 0)
                    }
                }
            })
        }

        return LiftedImageData(lifted!!)
    }
    // endregion

    // region IAnchorLiftModule

    override fun acceptsLifted(lifted: ILiftedData) = true

    override fun anchorLifted(lifted: ILiftedData, trans: Transform?) {
        workspace.undoEngine.performMaskedImageAction("Anchor Lifted", arranged, null, { built, mask ->
            built.drawOnComposite { gc->
                if(trans != null)
                    gc.preTransform(trans)
                lifted.draw(gc)
            }
        })
    }

    // endregion

    // region IColorChangeModule

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        workspace.undoEngine.performMaskedImageAction("ChangeColor", arranged, mask, { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { it.drawer.changeColor(from, to, mode) }
                else -> built.rawAccessComposite {
                    mask.doMasked(it, built.tWorkspaceToComposite) { it.drawer.changeColor(from, to, mode) }
                }
            }
        })
    }

    // endregion

    // region IFillModule
    override fun fill(x: Int, y: Int, color: Color): Boolean {
        workspace.undoEngine.performMaskedImageAction("ChangeColor", arranged, mask, { built, mask ->
            when( mask) {
                null -> built.rawAccessComposite {
                    val p = built.tWorkspaceToComposite.apply(Vec2(x.f,y.f))
                    it.drawer.fill(p.x.floor, p.y.floor, color)
                }
                else -> built.rawAccessComposite {
                    mask.doMaskedRequiringTransform(it, built.tWorkspaceToComposite, color) { it, tImageToFloating ->
                        val p =  tImageToFloating.apply(built.tWorkspaceToComposite.apply(Vec2(x.f,y.f)))
                        it.drawer.fill(p.x.floor,p.y.floor, color)

                    }
                }
            }
        })
        return true
    }
    // endregion

    override fun transform(trans: Transform) {
        val mask = mask

        val rect = when {
            mask == null -> Rect(arranged.handle.width, arranged.handle.height)
            mask.transform == null -> Rect(mask.width, mask.height)
            else -> MathUtil.circumscribeTrans(Rect(mask.width, mask.height), mask.transform)
        }

        val cx = rect.x + rect.width /2f
        val cy = rect.y + rect.height /2f

        val effectiveTrans = Transform.TranslationMatrix(cx,cy) * trans * Transform.TranslationMatrix(-cx,-cy)

        if( mask == null) {
            workspace.undoEngine.performAndStore(object: ImageAction(arranged) {
                override val description: String get() = "Transform"

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
            workspace.undoEngine.doAsAggregateAction("Lift and Transform") {
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
            val cx = medium.width / 2f
            val cy = medium.height / 2f

            val effectiveTrans = Transform.TranslationMatrix(cx,cy) * tool.transform * Transform.TranslationMatrix(-cx,-cy)

            gc.transform = effectiveTrans
            arranged.handle.medium.render(gc)
        }

        return MathUtil.circumscribeTrans(Rect(arranged.handle.width, arranged.handle.height), arranged.tMediumToWorkspace)
    }

    override fun stepManipulatingTransform() {
        workspace.compositor.triggerCompositeChanged()
    }
    override fun endManipulatingTransform() {
        workspace.compositor.compositeSource = null
    }

    //endregion
}