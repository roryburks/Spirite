package spirite.base.image_data.animations.ffa

import spirite.base.image_data.GroupTree
import spirite.base.image_data.ImageWorkspace
import spirite.base.util.UndoableDelegate
import spirite.hybrid.MDebug
import java.util.*

abstract class FFALayer(
        internal var context: FixedFrameAnimation
)
{
    abstract fun moveFrame( frameToMove: FFAFrame, frameRelativeTo:FFAFrame?, above: Boolean)


    val start = 0
    val end : Int get() {
        var caret = 0
        var i=0
        while( i < frames.size) {
            caret += frames[i].length
            if( frames[i].marker == FFAFrameStructure.Marker.START_LOCAL_LOOP)
                while( frames[++i].marker != FFAFrameStructure.Marker.END_LOCAL_LOOP);
            ++i
        }
        return caret
    }

    val workspace = context.context

    var name :String? = null
    var asynchronous = false

    protected val _frames = ArrayList<FFAFrame>()
    val frames : List<FFAFrame> get() { return _frames.toList()}


    // region Frame Access
    fun getFrameForMet( met: Int, noLoop: Boolean = false) : FFAFrame? {
        if( !frames.any())
            return null

        return _getFrameFromLocalLoop(0, met, noLoop)
    }
    private fun _getFrameFromLocalLoop( start: Int, offset: Int, noLoop: Boolean) : FFAFrame? {
        var index = start
        var caret = 0
        var loopLen = 0


        while( true) {
            val frame = frames[index++]
            loopLen += frame.length
            if( offset - caret < frame.length) {
                return when( frame.marker) {
                    FFAFrameStructure.Marker.START_LOCAL_LOOP -> _getFrameFromLocalLoop( index, offset-caret, noLoop)
                    FFAFrameStructure.Marker.FRAME  -> if (frame.isInGap(offset-caret)) null else frame
                    FFAFrameStructure.Marker.END_LOCAL_LOOP -> {
                        MDebug.handleWarning(MDebug.WarningType.STRUCTURAL, this, "Malformed Animation (END_LOCAL_LOOP with length > 1)")
                        null
                    }
                }
            }
            if( frame.marker == FFAFrameStructure.Marker.START_LOCAL_LOOP)
                while( frames[index].marker != FFAFrameStructure.Marker.END_LOCAL_LOOP) index++

            if( index == frames.size) {
                if( noLoop || loopLen == 0)
                    return null
                index = 0
            }

            caret += frame.length
        }
    }

    //endregion

    // =========
    // ==== Frame Extension Functions
    inner class FFAFrame(
            private val structure: FFAFrameStructure
    )
    {
        val end: Int get() {return start + length}
        val start: Int get() {
            val carets = Stack<Int>()
            carets.push(0)
            frames.iterator().forEach {
                if( it == this)
                    return carets.pop()
                carets.push( carets.pop() + it.length)
                if( it.marker == FFAFrameStructure.Marker.START_LOCAL_LOOP)
                    carets.push( carets.peek() - it.length)
                if( it.marker == FFAFrameStructure.Marker.END_LOCAL_LOOP)
                    carets.pop()
            }
            return Integer.MIN_VALUE
        }
        val loopDepth:Int get() {
            var depth = 0
            _frames.forEach {
                when {
                    it == this                              -> return depth
                    it.marker == FFAFrameStructure.Marker.START_LOCAL_LOOP    -> ++depth
                    it.marker == FFAFrameStructure.Marker.END_LOCAL_LOOP      -> --depth
                }
            }
            return 0
        }
        val next: FFAFrame? get() {
            val i = _frames.indexOf(this)
            if( i == -1 || (i+1) >= _frames.size)
                return null
            return _frames[i+1]
        }
        val workspace : ImageWorkspace get() = context.context
        val layerContext : FFALayer get() = this@FFALayer

        val node : GroupTree.Node? get() = structure.node
        val marker : FFAFrameStructure.Marker get() = structure.marker
        var gapBefore : Int by UndoableDelegate({structure.gapBefore = it; context._triggerChange()}, {structure.gapBefore}, ::workspace,"Changing Frame Gap")
        var gapAfter : Int by UndoableDelegate({structure.gapAfter = it; context._triggerChange()}, {structure.gapAfter}, ::workspace,"Changing Frame Gap")
        var innerLength : Int by UndoableDelegate({structure.length = it; context._triggerChange()}, {structure.length}, ::workspace,"Changing Frame Gap")
        var length : Int
            get() = innerLength + gapBefore + gapAfter
            set(value) = run { innerLength = value - gapBefore - gapAfter}

        fun isInGap( internalMet: Int) : Boolean {
            return (internalMet < gapBefore || (length-1) - internalMet < gapAfter)
        }
    }
}