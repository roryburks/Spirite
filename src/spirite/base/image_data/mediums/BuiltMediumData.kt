package spirite.base.image_data.mediums

import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.MediumHandle
import spirite.base.util.linear.MatTrans
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

enum class MediumSpaces {
    SOURCE,
    DESTINATION,
    COMPOSITE,
    SCREEN
}

abstract class BuiltMediumData(
    buildingMediumData: BuildingMediumData
)
{
    val handle: MediumHandle = buildingMediumData.handle
    protected val trans: MatTrans = buildingMediumData.trans
    protected val invTrans by lazy {
        trans.createInverse()
    }

    val sourceToComposite : MatTrans by lazy { _sourceToComposite}
    val screenToSource : MatTrans by lazy { _screenToSource }
    val screenToComposite : MatTrans by lazy {
        compositeToScreen.createInverse()
    }
    val compositeToScreen : MatTrans by lazy {
        val trans = MatTrans(compositeToSource)
        trans.preConcatenate(sourceToScreen)
        trans
    }

    val compositeToSource : MatTrans by lazy {
        sourceToComposite.createInverse()
    }
    val sourceToScreen: MatTrans by lazy {
        screenToSource.createInverse()
    }

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