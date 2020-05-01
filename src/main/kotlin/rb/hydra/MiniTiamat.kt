package rb.hydra

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException

fun <T> Sequence<T>.miniTiamatGrindSync(headCount: Int = 4, minimizer: (T)->Double)
        = runBlocking { miniTiamatGrind(headCount, minimizer) }

/***
 * Mini Tiamat's job is to take a Sequence of objects and find the smallest among them be independently calculating
 * along headCount different threads
 */
suspend fun <T> Sequence<T>.miniTiamatGrind(
        headCount: Int = 4, minimizer: (T)->Double) : Pair<Double,T>?
{
    val heads = List(headCount) { MiniTiamatHead<T>() }
    val channel = Channel<T>()

    val tasks = heads
            .map { head -> head.async {
                try {
                    while (true) {
                        val t = channel.receive()
                        val thisSize = minimizer(t)
                        if (thisSize < head.size) {
                            head.size = thisSize
                            head.t = t
                        }
                    }
                }catch (crce : ClosedReceiveChannelException){}
            } }

    forEach { channel.send(it) }
    channel.close()

    tasks.awaitAll()

    val min = heads.minBy { it.size }
    return if( min?.t == null) null else Pair(min.size, min.t!!)
}

private class MiniTiamatHead<T> : CoroutineScope by CoroutineScope(Dispatchers.Default){
    var t: T? = null
    var size: Double = Double.MAX_VALUE
}