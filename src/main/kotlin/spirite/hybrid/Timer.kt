package spirite.hybrid

import spirite.pc.JOGL.JOGLProvider
import javax.swing.SwingUtilities

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
        val timer = javax.swing.Timer( waitMilli) {
            JOGLProvider.context.makeCurrent()
            action.invoke()
            JOGLProvider.context.release()
        }
        if( repeat)
            timer.isRepeats = true
        timer.start()
        return SwTimer(timer)
    }

    override val currentMilli: Long get() = System.currentTimeMillis()
}