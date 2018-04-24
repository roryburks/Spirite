package spirite.base.imageData.drawer

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.GraphicsContext.Composite.DST_OUT
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
import spirite.base.util.linear.Transform

class DefaultImageDrawer(
        val arranged: ArrangedMediumData)
    :IImageDrawer,
        IStrokeModule,
        IInvertModule,
        IClearModule,
        ILiftSelectionModule,
        IAnchorLiftModule,
        IColorChangeModule
{

    val workspace : IImageWorkspace get() = arranged.handle.workspace
    val mask get() = workspace.selectionEngine.selection

    // region IStrokeModule
    private var strokeBuilder : StrokeBuilder? = null

    override fun canDoStroke(method: Method) = true

    override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
        val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
        strokeBuilder = StrokeBuilder(strokeDrawer, params, arranged)

        workspace.compositor.compositeSource = CompositeSource(
                arranged,
                {strokeDrawer.draw(it)})

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
    // endregion


    // region IInvertModule
    override fun invert() {
        workspace.undoEngine.performMaskedImageAction("Invert", arranged, mask, { built, mask ->
            when( mask) {
                null -> built.rawAccessComposite { it.drawer.invert() }
                else -> built.rawAccessComposite { mask.doMasked(it, {it.drawer.invert()}, built.tWorkspaceToComposite) }
            }
        })
    }
    // endregion


    // region ISelectionModule
    override fun clear() {
        workspace.undoEngine.performMaskedImageAction("Invert", arranged, mask, { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { it.graphics.clear() }
                else -> built.rawAccessComposite { mask.doMasked(it, { it.graphics.clear() }, built.tWorkspaceToComposite) }
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

    // region IColorChangeModuke

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        println("cc")
        workspace.undoEngine.performMaskedImageAction("ChangeColor", arranged, mask, { built, mask ->
            when (mask) {
                null -> built.rawAccessComposite { it.drawer.changeColor(from, to, mode) }
                else -> built.rawAccessComposite { mask.doMasked(it, { it.drawer.changeColor(from, to, mode) }, built.tWorkspaceToComposite) }
            }
        })
    }

    // endregion
}