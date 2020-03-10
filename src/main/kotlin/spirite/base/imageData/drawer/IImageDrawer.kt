package spirite.base.imageData.drawer

import rb.glow.color.Color
import rb.glow.color.SColor
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.imageData.deformation.IDeformation
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.selection.ILiftedData
import spirite.base.imageData.selection.ISelectionExtra
import spirite.base.imageData.selection.Selection
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.ImageAction
import spirite.base.pen.PenState
import spirite.base.pen.stroke.StrokeParams
import spirite.base.util.linear.Rect


fun IUndoEngine.performMaskedImageAction(description: String, arranged: ArrangedMediumData, mask: Selection?, action: (BuiltMediumData, Selection?)->Any? )
{
    performAndStore(object: ImageAction(arranged) {
        override val description: String get() = description

        override fun performImageAction(built: BuiltMediumData) {
            action(built, mask)
        }
    })
}

interface IImageDrawer {
    // Modules: an Image Drawer may implement these or they may not.  Not implementing them means
    //	that the Drawer is incapable of performing these draw actions (e.g. because it doesn't
    //	make sense for the data type).

    interface IStrokeModule {
        // EG: some Drawers might be able to erase, but not draw
        fun canDoStroke(method: StrokeParams.Method): Boolean
        fun startStroke(params: StrokeParams, ps: PenState): Boolean
        fun stepStroke(ps: PenState)
        fun endStroke()
    }

    interface IClearModule {
        fun clear()
    }

    interface IFillModule {
        fun fill(x: Int, y: Int, color: Color): Boolean
    }

    interface IFlipModule {
        // Note: Theoretically, a Drawer could implement Flipping without having to implement full matrix transformation
        fun flip(horizontal: Boolean)
    }
    interface ITransformModule : IFlipModule {
        fun transform(trans: ITransformF, centered: Boolean = true)

        fun startManipulatingTransform() : Rect?
        fun stepManipulatingTransform()
        fun endManipulatingTransform()

        override fun flip(horizontal: Boolean) {
            transform(when( horizontal) {
                true -> ImmutableTransformF.Scale(-1f,1f)
                false -> ImmutableTransformF.Scale(1f, -1f)
            })
        }
    }

    interface IColorChangeModule {
        fun changeColor(from: Color, to: Color, mode: ColorChangeMode)
    }

    interface IInvertModule {
        fun invert()
    }


//    interface IWeightEraserModule {
//        fun startWeightErase(precise: Boolean)
//        fun endWeightErase()
//        fun weightErase(xi: Float, yi: Float, wf: Float)
//    }
//
    interface IMagneticFillModule {
        val magFillXs: FloatArray
        val magFillYs: FloatArray
        fun startMagneticFill()
        fun endMagneticFill(color: SColor, mode: MagneticFillMode)
        fun anchorPoints(x: Float, y: Float, r: Float, locked: Boolean, relooping: Boolean)
        fun erasePoints(x: Float, y: Float, r: Float)
    }

    interface IMagneticEraseModule {
        fun erase( x: Float, y: Float)
    }

    interface ILiftSelectionModule {
        fun liftSelection(selection: Selection, clearLifted: Boolean = true): ILiftedData?
        fun getSelectionExtra(selection: Selection) : ISelectionExtra? = null
    }

    interface IAnchorLiftModule {
        fun acceptsLifted(lifted: ILiftedData): Boolean
        fun anchorLifted(lifted: ILiftedData, tMediumToLifted: ITransformF?)
    }

    interface IDeformDrawer {
        fun deform(deformation: IDeformation)
    }
//
//    interface IBoneDrawer {
//        fun contort(bone: BaseBone, to: Interpolator2D)
//    }
//
//    interface IPuppetBoneDrawer {
//        fun grabBone(xi: Int, yi: Int, width: Float): BaseBone
//        fun makeBone(x1: Float, y1: Float, x2: Float, y2: Float)
//    }
}

object NillImageDrawer : IImageDrawer