package rb.global

interface ILogger
{
    fun logTrace(message: String)
    fun logInformation(message: String)
    fun logWarning(message: String, exception: Exception? = null)
    fun logError(message: String, exception: Exception? = null)
}

object ConsoleLogger : ILogger {
    override fun logTrace(message: String) {
        println("[T]:$message")
    }

    override fun logInformation(message: String) {
        println("[I]:$message")
    }

    override fun logWarning(message: String, exception: Exception?) {
        println("[W]:$message")
    }

    override fun logError(message: String, exception: Exception?) {
        println("[E]:$message")
    }
}