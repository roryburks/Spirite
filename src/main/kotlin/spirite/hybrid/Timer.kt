package spirite.hybrid

interface ITimerEngine {
    fun createTimer(waitMilli : Int, repeat : Boolean = false,  action: ()-> Unit) : ITimer
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
    override fun createTimer(waitMilli: Int, repeat: Boolean, action: () -> Unit) : ITimer{
        val timer = javax.swing.Timer( waitMilli, {action.invoke()})
        if( repeat)
            timer.isRepeats = true
        timer.start()
        return SwTimer(timer)
    }

    override val currentMilli: Long get() = System.currentTimeMillis()
}