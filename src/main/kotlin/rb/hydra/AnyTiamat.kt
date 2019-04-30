package rb.hydra

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlin.coroutines.CoroutineContext


fun <T> Sequence<T>.anyTiamatGrindSync(headCount: Int = 4, lambda: (T) -> Boolean)
        = runBlocking { anyTiamatGrind(headCount, lambda) }

suspend fun <T> Sequence<T>.anyTiamatGrind(
        headCount: Int = 4, lambda: (T) -> Boolean) : Boolean
{
    val heads = List(headCount) { AnyTiamatHead<T>() }
    val channel = Channel<T>()

    val tasks = heads
            .map { head -> head.async {
                try {
                    while (true) {
                        val t = channel.receive()
                        if( lambda(t)) {
                            channel.close()
                            head.found = true
                        }
                    }
                }catch (crce : ClosedReceiveChannelException){}
            } }

    try {
        forEach { channel.send(it) }
        channel.close()
    }catch (crce : ClosedSendChannelException){}

    tasks.awaitAll()

    return heads.any { it.found }
}

private class AnyTiamatHead<T>() : CoroutineScope {
    var found: Boolean = false
    val job = Job()
    override val coroutineContext: CoroutineContext get() = newSingleThreadContext("test") + job
}