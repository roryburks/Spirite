package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.imageData.BuildingMediumData
import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MatrixSpace
import spirite.base.util.linear.Transform
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

abstract class BuiltMediumData(
        buildingMediumData: BuildingMediumData
)
{
    private val SOURCE = "src"
    private val COMPOSITE = "comp"
    private val SCREEN = "screen"

    val handle: MediumHandle = buildingMediumData.handle
    protected val trans: Transform = buildingMediumData.trans
    protected val invTrans by lazy {
        trans.invert()
    }

    private val matrixSpace by lazy{
        MatrixSpace(
                mapOf(
                        Pair(SOURCE, COMPOSITE) to _sourceToComposite,
                        Pair( SCREEN, SOURCE) to _screenToSource
                ))
    }

    val screenToComposite: Transform get() {return matrixSpace.convertSpace(SCREEN,COMPOSITE)}
    val compositeToScreen : Transform get() {return matrixSpace.convertSpace(COMPOSITE,SCREEN)}
    val screenToSource : Transform get() {return matrixSpace.convertSpace(SCREEN,SOURCE)}
    val sourceToScreen: Transform get() {return matrixSpace.convertSpace(SOURCE,SCREEN)}
    val sourceToComposite : Transform get() {return matrixSpace.convertSpace(SOURCE,COMPOSITE)}
    val compositeToSource : Transform get() {return matrixSpace.convertSpace(COMPOSITE,SOURCE)}


    // val screenToDynamic

    protected abstract val _sourceToComposite: Transform
    protected abstract val _screenToSource: Transform

    abstract val compositeWidth: Int
    abstract val compositeHeight: Int
    abstract val sourceWidth: Int
    abstract val sourceHeight: Int


    // Todo: Once a greater portion of the code is in Kotlin, replace these
    // Todo: with (GraphicsContext) -> Unit, etc
    abstract protected fun _doOnGC(doer : (GraphicsContext) -> Unit)
    abstract protected fun _doOnRaw( doer: (RawImage) -> Unit)


    private var doing = false
    fun doOnGC( doer : (GraphicsContext) -> Unit) {
        if( doing) {
            MDebug.handleError(ErrorType.STRUCTURAL,"Tried to recursively check-out")
            return
        }
        doing = true
        _doOnGC(doer)
        doing = false

        handle.refresh()
    }

    fun doOnRaw( doer: (RawImage) -> Unit) {
        if( doing) {
            MDebug.handleError(ErrorType.STRUCTURAL,"Tried to recursively check-out")
            return
        }
        doing = true
        _doOnRaw(doer)
        doing = false

        handle.refresh()
    }

}