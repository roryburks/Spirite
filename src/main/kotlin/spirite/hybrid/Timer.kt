package spirite.hybrid

interface ITimerEngine {
    fun createTimer( action: ()-> Unit, waitMilli : Int, repeat : Boolean = false) : ITimer
    val currentMilli : Long
}

interface ITimer {
    fun stop()
}

class SwTimer(val jtimer : javax.swing.Timer) : ITimer{
    override fun stop() {
        jtimer.stop()
    }
}

object SwTimerEngine : ITimerEngine {
    override fun createTimer(action: () -> Unit, waitMilli: Int, repeat: Boolean) : ITimer{
        val timer = javax.swing.Timer( waitMilli, {action.invoke()})
        if( repeat)
            timer.isRepeats = true
        timer.start()
        return SwTimer(timer)
    }

    override val currentMilli: Long get() = System.currentTimeMillis()
}