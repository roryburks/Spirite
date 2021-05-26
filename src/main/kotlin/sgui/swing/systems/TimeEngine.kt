package sgui.swing

import sgui.core.systems.ITimer
import sgui.core.systems.ITimerEngine
import javax.swing.Timer

class SwTimer(val jtimer : Timer) : ITimer {
    override fun stop() {
        jtimer.stop()
    }
}

object SwTimerEngine : ITimerEngine {
    override fun createTimer(waitMilli: Int, repeat: Boolean, action: () -> Unit) : ITimer {
        val timer = Timer( waitMilli) {action.invoke()}
        if( repeat)
            timer.isRepeats = true
        timer.start()
        return SwTimer(timer)
    }

    override val currentMilli: Long get() = System.currentTimeMillis()
}