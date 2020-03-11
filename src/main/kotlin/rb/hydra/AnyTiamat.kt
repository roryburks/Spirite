package rb.hydra

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException


fun <T> Sequence<T>.anyTiamatGrindSync(headCount: Int = 4, lambda: (T) -> Boolean)
        = runBlocking { anyTiamatGrind(headCount, lambda) }

suspend fun <T> Sequence<T>.anyTiamatGrind(
        headCount: Int = 4, lambda: (T) -> Boolean) : Boolean
{
    val heads = List(headCount) { AnyTiamatHead() }
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

private class AnyTiamatHead : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    var found: Boolean = false
}