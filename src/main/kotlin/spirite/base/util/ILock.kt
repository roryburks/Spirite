package spirite.base.util

interface ILock {
    fun withLock( run: ()->Any? )
}