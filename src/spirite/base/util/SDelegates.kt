//package spirite.base.util
//
//import spirite.base.imageData.ImageWorkspace
//import kotlin.reflect.KProperty
//
//class UndoableDelegate <T>(
//        private val setter : (T) -> Unit,
//        private val getter : () -> T,
//        private val workspaceGetter : KProperty<ImageWorkspace>,
//        private val changeDescription: String
//)
//{
//    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
//        return getter()
//    }
//
//    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
//        val oldValue = getter()
//        if( oldValue == newValue) return
//
//        val workspace = workspaceGetter.getter.call()
//        workspace.undoEngine.performAndStore( object: UndoEngine.NullAction() {
//            override fun performAction() {
//                setter( newValue)
//            }
//
//            override fun undoAction() {
//                setter( oldValue)
//            }
//
//            override fun getDescription(): String {
//                return changeDescription
//            }
//        })
//    }
//}