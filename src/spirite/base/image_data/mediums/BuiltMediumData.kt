package spirite.base.image_data.mediums

import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.MediumHandle
import spirite.base.util.linear.MatTrans
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

abstract class BuiltMediumData(
    buildingMediumData: BuildingMediumData
)
{
    val handle: MediumHandle = buildingMediumData.handle
    protected val trans: MatTrans = buildingMediumData.trans
    protected val invTrans by lazy {
        trans.createInverse()
    }

    abstract val drawTrans: MatTrans
    abstract val drawWidth: Int
    abstract val drawHeight: Int

    abstract val sourceTransform : MatTrans
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