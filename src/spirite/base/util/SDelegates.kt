package spirite.base.util

import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.UndoEngine
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class UndoableDelegate <T>(
        private val maskedProperty : KMutableProperty<T>,
        private val workspaceGetter : KProperty<ImageWorkspace>,
        private val changeDescription: String,
        private val onChangeTrigger: ((T) ->Unit)? =  null
)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return maskedProperty.getter.call()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        val oldValue = maskedProperty.getter.call()
        if( oldValue == newValue) return

        val workspace = workspaceGetter.getter.call()
        workspace.undoEngine.performAndStore( object: UndoEngine.NullAction() {
            override fun performAction() {
                maskedProperty.setter.call( newValue)
                onChangeTrigger?.invoke(newValue)
            }

            override fun undoAction() {
                maskedProperty.setter.call( oldValue)
                onChangeTrigger?.invoke(oldValue)
            }

            override fun getDescription(): String {
                return changeDescription
            }
        })
    }
}