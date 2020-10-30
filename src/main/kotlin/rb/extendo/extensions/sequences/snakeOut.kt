package rb.extendo.extensions.sequences

fun Int.snakeOut(i: Int = 1, startUp: Boolean = true) : Sequence<Int> = SnakeOutSequence(this, i, startUp)

private class SnakeOutSequence(val start: Int, val met: Int, val startUp: Boolean) : Sequence<Int>
{
    override fun iterator() = It()

    inner class It :  Iterator<Int> {
        var up = !startUp
        var spin = 0

        override fun hasNext() = true

        override fun next(): Int {
            return if( up) {
                up = false
                val ret = start + spin
                if( !startUp) ++spin
                ret
            } else {
                up =  true
                val ret = start - spin
                if( startUp) ++spin
                ret
            }
        }

    }
}

