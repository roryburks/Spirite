package spirite.base.image_data.animations.ffa

import spirite.base.image_data.GroupTree
import spirite.base.image_data.UndoEngine
import java.util.*

class FFALayer(
        includeSubtrees: Boolean, private var context: FixedFrameAnimation
)
{
    var start = 0; private set
    var end = 0; private set
    var name :String? = null; get() = field ?: groupLink?.name ?: "Unnamed Layer"
    var asynchronous = false

    private val _frames = ArrayList<FFAFrame>()
    val frames : List<FFAFrame> get() { return _frames.toList()}

    private var nodeLinks = HashMap<GroupTree.Node, FFAFrame>()



    var includeSubtrees = includeSubtrees
    set(value)  {
        val oldInclude = field
        context.context.undoEngine.performAndStore( object: UndoEngine.NullAction() {
            override fun performAction() {
                field = value
                context._triggerChange()
            }
            override fun undoAction() {
                field = oldInclude
                context._triggerChange()
            }
        })
    }


    var groupLink : GroupTree.GroupNode? = null
    set(value)  {

    }

    // ===========================
    // ==== Link Interpretation
    private fun groupLinkUdated() {
        if( groupLink != null) {
            var oldMap = nodeLinks
            var newMap = HashMap<GroupTree.Node, FFAFrame>()
            _frames.clear()
            _gluRec(oldMap, newMap, groupLink!!)

            nodeLinks = newMap
        }
    }
    private fun _gluRec(
            oldMap: HashMap<GroupTree.Node, FFAFrame>,
            newMap: HashMap<GroupTree.Node, FFAFrame>,
            node: GroupTree.GroupNode):Int
    {
        var len=0

        node.children.reversed().forEach  {
            if( it is GroupTree.GroupNode && includeSubtrees) {
                val usingNew = (oldMap == null || !oldMap.containsKey(it))
                val solFrame = if( usingNew)
                    FFAFrame(0, it, FFAFrameAbstract.Marker.START_LOCAL_LOOP)
                    else oldMap[it]!!

                _frames.add(solFrame)
                val subLen = _gluRec( oldMap, newMap, it)
                if( usingNew || solFrame.length < subLen)
                    solFrame.length = subLen
                _frames.add( FFAFrame(0, null, FFAFrameAbstract.Marker.END_LOCAL_LOOP))

                len += solFrame.length
                newMap.put( it, solFrame)
            }
            else if( it is GroupTree.LayerNode) {
                val newFrame =  if (oldMap == null || !oldMap.containsKey(it))
                    FFAFrame( 0, it, FFAFrameAbstract.Marker.FRAME)
                    else oldMap[it]!!
                _frames.add(newFrame)

                len += newFrame.length
                newMap.put(it, newFrame)
            }
        }
        return len
    }


    // =========
    // ==== Frame Extension Functions
    inner class FFAFrame(
            length: Int,
            node: GroupTree.Node?,
            marker: Marker,
            gapBefore: Int = 0,
            gapAfter: Int = 0)
        : FFAFrameAbstract(node, marker, length, gapBefore, gapAfter)
    {
        val end: Int get() {return start + length}
        val start: Int get() {
            val carets = Stack<Int>()
            frames.iterator().forEach {
                if( it == this)
                    return carets.pop()
                carets.push( carets.pop() + it.length)
                if( it.marker == FFAFrameAbstract.Marker.START_LOCAL_LOOP)
                    carets.push( carets.peek() - it.length)
                if( it.marker == FFAFrameAbstract.Marker.END_LOCAL_LOOP)
                    carets.pop()
            }
            return Integer.MIN_VALUE
        }

        fun changeFrameProperties(_length: Int = -1, _gapBefore: Int = -1, _gapAfter: Int = -1) {
            val oldLength = this.length
            val oldGapB = this.gapBefore
            val oldGapA = this.gapAfter
            val newLength = if( _length < 0) this.length else _length
            val newGapB = if( _gapBefore < 0) this.gapBefore else _gapBefore
            val newGapA = if( _gapAfter < 0) this.gapAfter else _gapAfter

            if( oldLength == newLength && oldGapA == newGapA && oldGapB == newGapB)
                return

            context.context.undoEngine.performAndStore( object: UndoEngine.NullAction() {
                override fun performAction() {
                    length = newLength
                    gapBefore = newGapB
                    gapAfter = newGapA
                    context._triggerChange()
                }
                override fun undoAction() {
                    length = oldLength
                    gapBefore = oldGapB
                    gapAfter = oldGapA
                    context._triggerChange()
                }
                override fun getDescription(): String {
                    return "Change Animation Lengths"
                }
            })
        }

        val loopDepth:Int get() {
            var depth = 0
            _frames.forEach {
                when {
                    it == this                              -> return depth
                    it.marker == Marker.START_LOCAL_LOOP    -> ++depth
                    it.marker == Marker.END_LOCAL_LOOP      -> --depth
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


    }
}