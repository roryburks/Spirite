package spirite.base.imageData.drawer

import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.selection.ILiftedData
import spirite.base.imageData.selection.Selection
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeParams
import spirite.base.util.linear.Transform


interface IImageDrawer {

//    abstract class MaskedImageAction protected constructor(data: ArrangedMediumData, protected val mask: Selection) : ImageAction(data)

    // Modules, an Image Drawer may implement these or they may not.  Not implementing them means
    //	that the Drawer is incapable of performing these draw actions (e.g. because it doesn't
    //	make sense for the data type).
    interface IStrokeModule {
        // EG: some Drawers might be able to erase, but not draw
        fun canDoStroke(method: StrokeParams.Method): Boolean
        fun startStroke(params: StrokeParams, ps: PenState): Boolean
        fun stepStroke(ps: PenState)
        fun endStroke()
    }
//
//    interface IClearModule {
//        fun clear()
//    }
//
    interface IFillModule {
        fun fill(x: Int, y: Int, color: Int, _data: ArrangedMediumData): Boolean
    }
//
//    interface IFlipModule {
//        fun flip(horizontal: Boolean)
//    }
//
//    interface IColorChangeModule {
//        fun changeColor(from: Int, to: Int, scope: ToolSchemes.ColorChangeScopes, mode: ToolSchemes.ColorChangeMode)
//    }
//
//    interface IInvertModule {
//        fun invert()
//    }
//
//    interface ITransformModule {
//        fun transform(trans: Transform)
//    }
//
//    interface IWeightEraserModule {
//        fun startWeightErase(precise: Boolean)
//        fun endWeightErase()
//        fun weightErase(x: Float, y: Float, w: Float)
//    }
//
//    interface IMagneticFillModule {
//        val magFillXs: FloatArray
//        val magFillYs: FloatArray
//        fun startMagneticFill()
//        fun endMagneticFill(color: Int, mode: MagneticFillMode)
//        fun anchorPoints(x: Float, y: Float, r: Float, locked: Boolean, relooping: Boolean)
//        fun erasePoints(x: Float, y: Float, r: Float)
//    }

    interface ILiftSelectionModule {
        fun liftSelection(selection: Selection): ILiftedData
    }

    interface IAnchorLiftModule {
        fun acceptsLifted(lifted: ILiftedData): Boolean
        fun anchorLifted(lifted: ILiftedData, trans: Transform?)
    }
//
//    interface IBoneDrawer {
//        fun contort(bone: BaseBone, to: Interpolator2D)
//    }
//
//    interface IPuppetBoneDrawer {
//        fun grabBone(x: Int, y: Int, width: Float): BaseBone
//        fun makeBone(x1: Float, y1: Float, x2: Float, y2: Float)
//    }
}

object NillImageDrawer : IImageDrawer