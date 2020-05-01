package rb.hydra

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException

/**
 * SelectiveTiamat is a coroutine version of filter (only use if your filter criteria are heavy and parallelable)
 */

fun <T> List<T>.selectiveTiamatGrindSync( headCount: Int = 4, filter: (T)->Boolean) : List<T>
        = runBlocking { selectiveTiamatGrind(headCount, filter) }

suspend fun <T> List<T>.selectiveTiamatGrind(
        headCount: Int = 4, filter: (T)->Boolean) : List<T>
{
    val heads = List(headCount){ CoroutineScope(Dispatchers.Default)}
    val channel = Channel<T>()
    val output = mutableListOf<T>()

    val tasks = heads
            .map { head -> head.async {
                try {
                    while(true) {
                        val t = channel.receive()
                        if( filter(t)) output.add(t)
                    }
                }catch (crce: ClosedReceiveChannelException){}
            } }

    forEach { channel.send(it) }
    channel.close()

    tasks.awaitAll()
    return output
}
