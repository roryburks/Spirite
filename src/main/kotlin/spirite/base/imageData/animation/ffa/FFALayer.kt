package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.*
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.undo.IUndoEngineFeed
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoableChangeDelegate
import spirite.base.imageData.undo.performAndStore


interface IFFALayerLinked {
    fun groupLinkUpdated()
    fun shouldUpdate(contract: FFAUpdateContract) : Boolean
}

abstract class FFALayer(
        override val anim : FixedFrameAnimation,
        private val _undoEngine : IUndoEngineFeed? = null,
        asynchronous: Boolean = false)
    :IFfaLayer
{
    override val start = 0
    override val end : Int get() {
        var caret = 0
        var i=0
        while( i < _frames.size) {
            caret += _frames[i].length
            if( _frames[i].marker == START_LOCAL_LOOP) {
                var inll = 1
                while(inll > 0)
                    when(_frames[++i].marker) {
                        END_LOCAL_LOOP -> inll--
                        START_LOCAL_LOOP -> inll++
                    }
            }
            ++i
        }
        return caret
    }

    override var asynchronous by UndoableChangeDelegate(
            asynchronous,
            _undoEngine,
            "Toggled Frame Layer Asynchronousness") {anim.triggerFFAChange(this)}

    protected val _frames = mutableListOf<FFAFrame>()
    override val frames : List<IFfaFrame> get() = _frames

    override fun getFrameFromLocalMet(met: Int, loop: Boolean) : FFAFrame? {
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
                        GAP -> frame
                        START_LOCAL_LOOP -> _sub(index, offset - caret)
                        FRAME -> frame
                        END_LOCAL_LOOP -> {
                            //MDebug.handleWarning(STRUCTURAL, "Malformed Animation (END_LOCAL_LOOP with length > 0)")
                            // TODO: Debug Better
                            println("Malformed Animation (END_LOCAL_LOOP with length > 0)")
                            null
                        }
                    }
                }
                if( frame.marker == START_LOCAL_LOOP) {
                    var inll = 1
                    while (inll > 1)
                        if(_frames[++index].marker != END_LOCAL_LOOP)inll++
                }

                if( index == _frames.size || frame.marker == END_LOCAL_LOOP) {
                    if( !loop || loopLen == 0)
                        return null
                    index = start
                }

                caret += frame.length
            }
        }

        return  _sub(0, met)
    }

    inner class FFAFrame(structure: FfaFrameStructure)
        :IFfaFrame
    {
        // region Calculations
        override val layer get() = this@FFALayer

        override val start: Int get() {
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
        val previous: FFAFrame? get() = _frames.getOrNull(_frames.indexOf(this) - 1)

        // endregion

        // region Structure
        var structure = structure ; internal  set

        val node get() = structure.node
        val marker get() = structure.marker
        override var length get() = structure.length
            set(value) { _undoEngine.performAndStore(FFAStructureChangeAction(structure.copy(length = value),"Changed Frame Length"))}

        private inner class FFAStructureChangeAction(
                val newStructure: FfaFrameStructure,
                override val description: String)
            : NullAction()
        {
            val oldStructure = structure

            override fun performAction() {structure = newStructure ; anim.triggerFFAChange(this@FFALayer)}
            override fun undoAction() {structure = oldStructure ; anim.triggerFFAChange(this@FFALayer)}
        }
        // endregion

        override fun getDrawList() = (node as? LayerNode)?.getDrawList() ?: emptyList()
    }

}