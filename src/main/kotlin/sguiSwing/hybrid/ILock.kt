package sguiSwing.hybrid

/**
 * The ILock interface encapsulates whatever locking/messaging system the underlying language libraries have access to.
 *
 * An IHybrid will implement CreateLock(object) which will produce an ILock distinct to that object.
 */

interface ILock {
    fun withLock( run: ()->Any? )
}

class JLock( val o: Any) : ILock {
    override fun withLock(run: () -> Any?) {
        synchronized( o, run)
    }
}