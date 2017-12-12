package spirite.base.image_data.mediums

import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.MediumHandle
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.MatrixSpace
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
    protected val trans: MatTrans = buildingMediumData.trans
    protected val invTrans by lazy {
        trans.createInverse()
    }

    private val matrixSpace by lazy{
        MatrixSpace(
            mapOf(
                    Pair(SOURCE, COMPOSITE) to _sourceToComposite,
                    Pair( SCREEN, SOURCE) to _screenToSource
            ))
    }

    val screenToComposite: MatTrans get() {return matrixSpace.convertSpace(SCREEN,COMPOSITE)}
    val compositeToScreen : MatTrans get() {return matrixSpace.convertSpace(COMPOSITE,SCREEN)}
    val screenToSource :MatTrans get() {return matrixSpace.convertSpace(SCREEN,SOURCE)}
    val sourceToScreen:MatTrans get() {return matrixSpace.convertSpace(SOURCE,SCREEN)}
    val sourceToComposite :MatTrans get() {return matrixSpace.convertSpace(SOURCE,COMPOSITE)}
    val compositeToSource :MatTrans get() {return matrixSpace.convertSpace(COMPOSITE,SOURCE)}


    // val screenToDynamic

    protected abstract val _sourceToComposite: MatTrans
    protected abstract val _screenToSource: MatTrans

    abstract val compositeWidth: Int
    abstract val compositeHeight: Int
    abstract val sourceWidth: Int
    abstract val sourceHeight: Int


    // Todo: Once a greater portion of the code is in Kotlin, replace these
    // Todo: with (GraphicsContext) -> Unit, etc
    abstract protected fun _doOnGC(doer : DoerOnGC)
    abstract protected fun _doOnRaw( doer: DoerOnRaw)


    private var doing = false
    fun doOnGC( doer : DoerOnGC) {
        if( doing) {
            MDebug.handleError(ErrorType.STRUCTURAL,"Tried to recursively check-out")
            return
        }
        handle.context!!.undoEngine.prepareContext( handle)
        doing = true
        _doOnGC(doer)
        doing = false

        handle.refresh()
    }

    fun doOnRaw( doer: DoerOnRaw) {
        if( doing) {
            MDebug.handleError(ErrorType.STRUCTURAL,"Tried to recursively check-out")
            return
        }
        handle.context!!.undoEngine.prepareContext( handle)
        doing = true
        _doOnRaw(doer)
        doing = false

        handle.refresh()
    }

}