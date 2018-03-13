package spirite.hybrid

import javax.swing.SwingUtilities

interface ITimerEngine {
    fun createTimer( action: ()-> Unit, waitMilli : Int, repeat : Boolean = false) : ITimer
}

interface ITimer {
    fun stop()
}

class STimer( val jtimer : javax.swing.Timer) : ITimer{
    override fun stop() {
        jtimer.stop()
    }
}

object STimerEngine : ITimerEngine {
    override fun createTimer(action: () -> Unit, waitMilli: Int, repeat: Boolean) : ITimer{
        val timer = javax.swing.Timer( waitMilli, {action.invoke()})
        if( repeat)
            timer.isRepeats = true
        timer.start()
        return STimer(timer)
    }
}