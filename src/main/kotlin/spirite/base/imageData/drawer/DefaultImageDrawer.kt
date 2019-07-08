package spirite.base.imageData.drawer

import rb.glow.Composite.*
import rb.glow.RawImage
import rb.glow.color.Color
import rb.glow.color.ColorUtil
import rb.glow.color.SColor
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import rb.vectrix.mathUtil.round
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.brains.toolset.MagneticFillMode.BEHIND
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.HandleCompositeSource
import spirite.base.imageData.mediums.IMedium
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
import kotlin.math.min


class DefaultImageDrawer(
        val arranged: ArrangedMediumData)
    :IImageDrawer,
        IStrokeModule by DefaultStrokeModule(arranged),
        IMagneticFillModule by DefaultMagFillModule(arranged),
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

        workspace.compositor.compositeSource = HandleCompositeSource(arranged, false ) { gc ->
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


class DefaultStrokeModule(val arranged: ArrangedMediumData) : IStrokeModule {
    val workspace : IImageWorkspace get() = arranged.handle.workspace
    val mask get() = workspace.selectionEngine.selection

    private var strokeBuilder : StrokeBuilder? = null

    override fun canDoStroke(method: Method) = true

    override fun startStroke(params: StrokeParams, ps: PenState): Boolean {
        val strokeDrawer = workspace.strokeProvider.getStrokeDrawer(params)
        val sb = StrokeBuilder(strokeDrawer, params, arranged)
        strokeBuilder = sb

        workspace.compositor.compositeSource = HandleCompositeSource(arranged) { strokeDrawer.draw(it) }

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

class DefaultMagFillModule(val arranged: ArrangedMediumData) : IMagneticFillModule {
    var fx: FloatCompactor? = null
    var fy: FloatCompactor? = null

    val workspace: IImageWorkspace get() = arranged.handle.workspace

    override val magFillXs: FloatArray get() = fx?.toArray() ?: FloatArray(0)
    override val magFillYs: FloatArray get() = fy?.toArray() ?: FloatArray(0)

    override fun startMagneticFill() {
        fx = FloatCompactor()
        fy = FloatCompactor()
    }

    override fun endMagneticFill(color: SColor, mode: MagneticFillMode) {
        val fillX = fx?.toArray() ?: return
        val fillY = fy?.toArray() ?: return
        val size = min(fillX.size, fillY.size)
        workspace.undoEngine.performAndStore(object: ImageAction(arranged) {
            override val description: String get() = "Magnetic Fill"

            override fun performImageAction(built: BuiltMediumData) {
                built.rawAccessComposite {
                    val gc = it.graphics
                    gc.color = color
                    if( mode == BEHIND)
                        gc.composite = DST_OVER
                    gc.fillPolygon(fillX.asList(), fillY.asList(), size)
                }
            }
        })

        fx = null
        fy = null
    }

    override fun anchorPoints(x: Float, y: Float, r: Float, locked: Boolean, relooping: Boolean) {
        val lockedColor = if( locked) workspace.paletteManager.activeBelt.getColor(0) else null
        val start = arranged.tMediumToWorkspace.apply(Vec2f(x,y))
        val sx = start.x.round
        val sy = start.y.round

        val medium = arranged.handle.medium

        for( tr in 1 until (r+1).floor) {
            for(snake in 0 until tr) {
                // Topleft->topright
                if (tryPixel(sx - tr + snake, sy - tr, medium, lockedColor)) return
                //TR->BR
                if (tryPixel(sx + tr, sy - tr + snake, medium, lockedColor)) return
                //BR->BL
                if (tryPixel(sx + tr - snake, sy + tr, medium, lockedColor)) return
                //BL->TL
                if (tryPixel(sx - tr, sy + tr - snake, medium, lockedColor)) return
            }
        }
    }

    private fun tryPixel(x: Int, y: Int, medium: IMedium, lockedColor: Color? ) : Boolean
    {
        val fx = fx ?: return true  // counter-intuitive, but returning true so we don't try anymore.  shouldn't come up
        val fy = fy ?: return true

        // no movement, don't bother checking
        if( fx.size > 1 && Math.abs(x - fx[fx.size - 1]) < 0.001f&& Math.abs(y - fy[fy.size - 1]) < 0.001f)
            return false

        val c = medium.getColor(x,y)
        if( (lockedColor != null && ColorUtil.colorDistance(c, lockedColor) < 25)
                || (lockedColor == null && c.alpha > 0.9))
        {
            fx.add(x.f)
            fy.add(y.f)
            return true
        }
        return false
    }

    override fun erasePoints(x: Float, y: Float, r: Float) {
        // TODO
    }

}