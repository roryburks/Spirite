package spirite.base.util

/**
 * The purpose of this class is to streamline the binding of UI to Data in which
 * both are listening for each other thus need to be locked out of endless loops
 */
class DataBinding<T> {
    private var lock = false
    internal var link: ChangeExecuter<T>? = null

    fun triggerUIChanged(newValue: T) {
        if (lock)
            return
        lock = true
        if (link != null)
            link!!.doUIChanged(newValue)
        lock = false
    }

    fun triggerDataChanged(newValue: T) {
        if (lock)
            return
        lock = true
        if (link != null)
            link!!.doDataChanged(newValue)
        lock = false
    }

    fun setLink(executer: ChangeExecuter<T>) {
        this.link = executer
    }

    interface ChangeExecuter<T> {
        fun doUIChanged(newValue: T)
        fun doDataChanged(newValue: T)
    }
}
