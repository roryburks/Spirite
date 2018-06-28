package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoEngine
import spirite.base.util.delegates.UndoableChangeDelegate
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

abstract class FFALayer( internal val context : FixedFrameAnimation) {
    private val undoEngine get() = context.workspace.undoEngine

    abstract fun moveFrame( frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean)

    val start = 0
    val end : Int get() {
        var caret = 0
        var i=0
        while( i < _frames.size) {
            caret += _frames[i].length
            if( _frames[i].marker == START_LOCAL_LOOP)
                while( _frames[++i].marker != END_LOCAL_LOOP);
            ++i
        }
        return caret
    }

    var asynchronous by UndoableChangeDelegate(false, context.workspace.undoEngine,
            "Toggled Frame Layer Asynchronousness", {context.triggerFFAChange(this)})

    protected val _frames = mutableListOf<FFAFrame>()
    val frames : List<FFAFrame> get() = _frames

    fun getFramFromLocalMet( met: Int, loop: Boolean = true) : FFAFrame? {
        if( !_frames.any())
            return null

        fun _sub( start: Int, offset: Int) : FFAFrame?{
            var index = start
            var caret = 0
            var loopLen = 0

            while (true){
                val frame = _frames[index++]
                loopLen += frame.length
                if( offset - caret < frame.length) {
                    return when( frame.marker) {
                        START_LOCAL_LOOP -> _sub(index, offset - caret)
                        FRAME -> if( frame.isInGap(offset-caret)) null else frame
                        END_LOCAL_LOOP -> {MDebug.handleWarning(STRUCTURAL, "Malformed Animation (END_LOCAL_LOOP with length > 1)"); null}
                    }
                }
                if( frame.marker == START_LOCAL_LOOP)
                    while (_frames[++index].marker != END_LOCAL_LOOP);

                if( index == _frames.size) {
                    if( !loop || loopLen == 0)
                        return null
                    index = 0
                }

                caret += frame.length
            }
        }

        return  _sub(0, met)
    }

    inner class FFAFrame(structure: FFAFrameStructure)
    {
        // region Calculations
        val start: Int get() {
            val carets = mutableListOf(0)
            _frames.forEach {
                if( it == this)
                    return carets.last()
                carets[carets.lastIndex] = carets[carets.lastIndex] + it.length

                when( it.marker) {
                    START_LOCAL_LOOP -> carets.add(carets.last() - it.length)
                    END_LOCAL_LOOP -> carets.remove(carets.lastIndex)
                    else -> {}
                }
            }
            return Integer.MIN_VALUE
        }
        val end get() = start + length

        val loopDepth: Int get() {
            var depth = 0
            _frames.forEach {
                when {
                    it == this -> return depth
                    it.marker == START_LOCAL_LOOP -> ++depth
                    it.marker == END_LOCAL_LOOP -> --depth
                }
            }
            return 0
        }

        val next: FFAFrame? get() = _frames.getOrNull(_frames.indexOf(this) + 1)

        internal fun isInGap( internalMet: Int) = (internalMet < gapBefore) || ((length-1) - internalMet < gapAfter)
        // endregion

        // region Structure
        var structure = structure ; internal  set

        val node get() = structure.node
        val marker get() = structure.marker
        var gapBefore get() = structure.gapBefore
            set(value) { undoEngine.performAndStore(FFAStructureChangeAction(structure.copy(gapBefore = value),"Changed Frame Gap Before"))}
        var gapAfter get() = structure.gapAfter
            set(value) { undoEngine.performAndStore(FFAStructureChangeAction(structure.copy(gapAfter = value),"Changed Frame Gap After"))}
        var innerLength get() = structure.length
            set(value) { undoEngine.performAndStore(FFAStructureChangeAction(structure.copy(length = value),"Changed Frame Inner Length"))}

        var length get() = innerLength + gapBefore + gapAfter
            set(value) {innerLength = value - gapBefore - gapAfter}

        private inner class FFAStructureChangeAction(
                val newStructure: FFAFrameStructure,
                override val description: String)
            : NullAction()
        {
            val oldStucture = structure

            override fun performAction() {structure = newStructure ; context.triggerFFAChange(this@FFALayer)}
            override fun undoAction() {structure = oldStucture ; context.triggerFFAChange(this@FFALayer)}
        }
        // endregion
    }

}