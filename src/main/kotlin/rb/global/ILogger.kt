package rb.global

interface ILogger
{
    fun logTrace(message: String)
    fun logInformation(message: String)
    fun logWarning(message: String, exception: Exception? = null)
    fun logError(message: String, exception: Exception? = null)
}