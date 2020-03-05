package spirite.base.imageData.mediums.magLev.selecting

import rb.extendo.extensions.stride
import rb.extendo.extensions.toLookup
import rb.glow.GraphicsContext
import rb.glow.color.Colors
import rb.vectrix.linear.*
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.imageData.selection.Selection
import spirite.base.util.debug.SpiriteException
import spirite.base.util.linear.Rect
import spirite.base.util.linear.RectangleUtil
import kotlin.math.min


class SimpleMaglevStrokeSelection(
        val context: MaglevMedium,
        val lines: List<MaglevStroke>,
        val tLinesToScreen: ITransformF
) : IMaglevSelection {
    override fun draw(gc: GraphicsContext) {
        gc.color = Colors.WHITE
        lines.forEach { stroke ->
            val dp = stroke.drawPoints
            val xs = (0 until stroke.drawPoints.length).step(10)
                    .map { dp.x[it] }
                    .toFloatArray()
            val ys = (0 until stroke.drawPoints.length).step(10)
                    .map{ dp.y[it]}
                    .toFloatArray()
            gc.drawPolyLine(xs, ys, min(xs.size, ys.size))
        }
    }

    override fun lift(): IMaglevLiftedData {
        TODO("Not yet implemented")
    }

    companion object {
        private val shallowThresh = 0.7f
        private val deepThresh = 0.7f
        private val deepThreshAlpha = 0.9f
        private val deepStride = 10

        fun FromArranged( arranged: ArrangedMediumData) : SimpleMaglevStrokeSelection
        {
            val medium = arranged.handle.medium
            val maglev = medium as? MaglevMedium ?: throw SpiriteException("Tried to lift a non-maglev medium into a Maglev Selection")
            if( arranged.selection == null){
                return SimpleMaglevStrokeSelection(
                        maglev,
                        maglev.things.filterIsInstance<MaglevStroke>(),
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

                val passingStrokes = maglev.things
                        .filterIsInstance<MaglevStroke>()
                        .filter { stroke ->
                            val dp = stroke.drawPoints
                            val pts = (0 until dp.length)
                                    .map { tMediumToSelection.apply(Vec2f(dp.x[it], dp.y[it])) }

                            passesShallowThreshold(pts) && passesDeepThreshold(pts)
                        }

                return SimpleMaglevStrokeSelection(
                        maglev,
                        passingStrokes,
                        arranged.tMediumToWorkspace)
            }
        }
    }
}