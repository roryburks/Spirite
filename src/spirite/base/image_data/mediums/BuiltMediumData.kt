//package spirite.base.image_data.mediums
//
//import spirite.base.graphics.GraphicsContext
//import spirite.base.image_data.MediumHandle
//import spirite.base.util.linear.MatTrans
//import spirite.hybrid.MDebug
//import spirite.hybrid.MDebug.ErrorType
//
//abstract class BuiltMediumData(
//    val handle: MediumHandle,
//    val trans: MatTrans
//)
//{
//    val invTrans = trans.createInverse()
//
//    abstract protected fun _doOnGC(doer : (GraphicsContext) -> Unit  )
//
//    private var doing = false
//    fun doOnGC( doer : (GraphicsContext) -> Unit ) {
//        if( doing) {
//            MDebug.handleError(ErrorType.STRUCTURAL,"Tried to recursively check-out")
//            return
//        }
//        handle.context!!.undoEngine.prepareContext( handle)
//        doing = true
//        _doOnGC(doer)
//        doing = false
//
//        handle.refresh()
//    }
//
//}