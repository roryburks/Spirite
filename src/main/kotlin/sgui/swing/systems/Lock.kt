package sgui.swing.systems

import sgui.core.systems.ILock


class JLock( val o: Any) : ILock {
    override fun withLock(run: () -> Any?) {
        synchronized( o, run)
    }
}