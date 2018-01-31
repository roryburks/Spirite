package spirite.base.imageData.mediums.drawer

import spirite.base.util.interpolation.Interpolator2D
import spirite.base.util.linear.Transform


interface IImageDrawer {
    // TODO

//    abstract class MaskedImageAction protected constructor(data: BuildingMediumData, protected val mask: SelectionMask) : ImageAction(data)
//
//    // Modules, an Image Drawer may implement these or they may not.  Not implementing them means
//    //	that the Drawer is incapable of performing these draw actions (e.g. because it doesn't
//    //	make sense for the data type).
//    interface IStrokeModule {
//        //public boolean strokeIsDrawing();	// SHOULD be true iff getStrokeEngine = null
//        val strokeEngine: StrokeEngine
//
//        fun canDoStroke(method: StrokeEngine.Method): Boolean    // EG: some Drawers might be able to erase, but not draw
//        fun startStroke(params: StrokeParams, ps: PenState): Boolean
//        fun stepStroke(ps: PenState)
//        fun endStroke()
//    }
//
//    interface IClearModule {
//        fun clear()
//    }
//
//    interface IFillModule {
//        fun fill(x: Int, y: Int, color: Int, _data: BuildingMediumData): Boolean
//    }
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
//
//    interface ILiftSelectionModule {
//        fun liftSelection(selection: SelectionMask): ALiftedData
//    }
//
//    interface IAnchorLiftModule {
//        fun acceptsLifted(lifted: ALiftedData): Boolean
//        fun anchorLifted(lifted: ALiftedData, trans: Transform)
//    }
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
