package spirite.base.imageData.mediums.magLev.selecting

import rb.extendo.extensions.stride
import rb.glow.Colors
import rb.glow.IGraphicsContext
import rb.glow.img.IImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.*
import sgui.hybrid.Hybrid
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.imageData.selection.ILiftedData
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.util.debug.SpiriteException
import spirite.base.util.linear.Rect
import kotlin.math.min

class SimpleStrokeMaglevLiftedData(
        val lines: List<MaglevStroke>,
        val tInternalToContext: ITransformF,
        val strokeProvider: IStrokeDrawerProvider)
    : IMaglevLiftedData
{
    override val image: IImage
    val dx: Int
    val dy: Int

    init {
        var x1 :Float? = null
        var y1: Float? = null
        var x2: Float? = null
        var y2: Float? = null
        lines.forEach { line ->
            val s = line.params.width
            val dp = line.drawPoints
            (0 until line.drawPoints.length).forEach {
                val x = dp.x[it]
                val y = dp.y[it]
                x1 = MathUtil.minOrNull(x1, x - s)
                y1 = MathUtil.minOrNull(y1, y - s)
                x2 = MathUtil.maxOrNull(x2, x + s)
                y2 = MathUtil.maxOrNull(y2, y + s)
            }
        }
        dx = x1?.floor ?: 0
        dy = y1?.floor ?: 0
        val dw = (x2?.floor?:0) - dx
        val dh = (y2?.floor?:0) - dy
//        val dw = 1000
//        val dh = 1000

        val img = Hybrid.imageCreator.createImage(dw, dh)
        val gc = img.graphics

        gc.translate(-dx.d, -dy.d)
        lines.forEach { line ->
            line.draw(gc, strokeProvider, dw, dh)
        }

//        Hybrid.imageIO.saveImage(img, File("C:/Bucket/ex.png"))

        image = img
    }

    override fun anchorOnto(other: MaglevMedium, arranged: ArrangedMediumData, tMediumToLifted: ITransformF) {
        if(!lines.any()) return

        // Short justification for this transform:
        // tInternalToContext stores the inverse of the initial transform of the selection
        // tMediumToLifted stores the new selection transform
        // so invert old then apply new = transformed the difference
        val tLineToOther = tInternalToContext * tMediumToLifted
        val transformedLines = lines.map { line ->
            val dp = line.drawPoints
            val tx = FloatArray(dp.length)
            val ty = FloatArray(dp.length)
            (0 until dp.length).forEach {
                val new = tLineToOther.apply(Vec2f(dp.x[it],dp.y[it]))
                tx[it] = new.xf
                ty[it] = new.yf
            }
            MaglevStroke(line.params, DrawPoints(tx, ty, dp.w)) // Nothing could possibly go wrong by just passing the width array...  right?
        }
        other.addThings(transformedLines,arranged, "Anchoring Lifted SimpleMaglevStroke to Maglev")
    }

    override fun draw(gc: IGraphicsContext) {
        gc.pushTransform()
        gc.preTransform(tInternalToContext)
        gc.renderImage(image,dx.d, dy.d)
        gc.popTransform()
    }

    override fun bake(transform: ITransformF): ILiftedData = SimpleStrokeMaglevLiftedData(
            lines,
            transform * tInternalToContext,
            strokeProvider)


    override fun getImageDrawer(workspace: IImageWorkspace) = NillImageDrawer

    override val width: Int get() = image.width
    override val height: Int get() = image.height
}

class SimpleMaglevStrokeSelectionExtra(
        val context: MaglevMedium,
        val lines: List<MaglevStroke>,
        val tInternalToContext: ITransformF)
    : IMaglevSelectionExtra
{
    override fun draw(gc: IGraphicsContext) {
        gc.pushTransform()
        gc.preTransform(tInternalToContext)
        gc.color = Colors.WHITE
        gc.alpha = 1f
        lines.forEach { stroke ->
            val dp = stroke.drawPoints
            val xs = (0 until stroke.drawPoints.length).step(10)
                    .map { dp.x[it].d }
            val ys = (0 until stroke.drawPoints.length).step(10)
                    .map{ dp.y[it].d}
            gc.drawPolyLine(xs, ys, min(xs.size, ys.size))
        }
        gc.popTransform()
    }

    override fun lift(arranged: ArrangedMediumData): IMaglevLiftedData? {
        if( !lines.any()) return null
        context.removeThings(lines, arranged, "Lifted Selection out of Maglev")
        return  SimpleStrokeMaglevLiftedData(lines, tInternalToContext, arranged.handle.workspace.strokeProvider)
    }

    companion object {
        private val shallowThresh = 0.7f
        private val deepThresh = 0.7f
        private val deepThreshAlpha = 0.9f
        private val deepStride = 10

        fun FromArranged( arranged: ArrangedMediumData) : SimpleMaglevStrokeSelectionExtra
        {
            val medium = arranged.handle.medium
            val maglev = medium as? MaglevMedium ?: throw SpiriteException("Tried to lift a non-maglev medium into a Maglev Selection")
            if( arranged.selection == null){
                return SimpleMaglevStrokeSelectionExtra(
                        maglev,
                        maglev.thingsMap.values.filterIsInstance<MaglevStroke>(),
                        arranged.tMediumToWorkspace)
            }
            else {
                // Two competing Transforms:
                //   Selection Transform transforming SelectionSpace -> WorkSpace
                //   Arrangement Transform transforming MediumSpece -> WorkSpace
                //   Thus to get a Transform from StrokeSpece = MediumSpace -> SelectionSpace
                //        tMediumToWorkspace * (tSelectionToWorkspace)^-1
                val tWorkspaceToSelection = arranged.selection.transform?.invert() ?: ImmutableTransformF.Identity
                val tMediumToSelection = arranged.tMediumToWorkspace * tWorkspaceToSelection

                val selectionBounds = Rect(0, 0, arranged.selection.width, arranged.selection.height)

                fun passesShallowThreshold(pts: List<Vec2f>) : Boolean {
                    val passCt = pts.count { selectionBounds.contains(it.xf.round, it.yf.round) }
                    return (passCt.f / pts.count()) > shallowThresh
                }

                fun passesDeepThreshold(pts: List<Vec2f>) : Boolean {
                    val ptsToCheck = pts.stride(deepStride)

                    val passCt = ptsToCheck
                            .count {
                                val color = arranged.selection.mask.getColor(it.xf.round, it.yf.round)
                                return color.alpha > deepThreshAlpha
                            }

                    return (passCt.f / ptsToCheck.count().f) > deepThresh
                }

                val passingStrokes = maglev.thingsMap.values
                        .filterIsInstance<MaglevStroke>()
                        .filter { stroke ->
                            val dp = stroke.drawPoints
                            val pts = (0 until dp.length)
                                    .map { tMediumToSelection.apply(Vec2f(dp.x[it], dp.y[it])) }

                            passesShallowThreshold(pts) && passesDeepThreshold(pts)
                        }

                return SimpleMaglevStrokeSelectionExtra(
                        maglev,
                        passingStrokes,
                        tMediumToSelection)
            }
        }
    }
}