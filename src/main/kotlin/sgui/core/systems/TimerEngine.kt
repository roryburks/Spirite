package sgui.core.systems

interface ITimerEngine {
    fun createTimer(waitMilli : Int, repeat : Boolean = false,  action: ()-> Unit) : ITimer
    val currentMilli : Long
}

interface ITimer {
    fun stop()
}