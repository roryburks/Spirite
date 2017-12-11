package spirite.base.image_data.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.image_data.MediumHandle
import spirite.base.util.linear.MatTrans
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

abstract class BuiltMediumData(
    val handle: MediumHandle,
    protected val trans: MatTrans
)
{
    protected val invTrans by lazy { trans.createInverse()}

    abstract val destTrans : MatTrans
    abstract val destWidth : Int
    abstract val destHeight : Int

    abstract val sourceTransform : MatTrans
    abstract val sourceWidth: Int
    abstract val sourceHeight: Int


    abstract protected fun _doOnGC(doer : (GraphicsContext) -> Unit  )
    abstract protected fun _doOnRaw( doer: (RawImage) -> Unit)

    private var doing = false
    fun doOnGC( doer : (GraphicsContext) -> Unit ) {
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

    fun doOnRaw( doer: (RawImage) -> Unit) {
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