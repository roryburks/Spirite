package spirite.base.imageData.view

import rb.vectrix.mathUtil.MathUtil
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.DefaultAnimCharMap
import spirite.base.imageData.animation.ffa.FfaLayerLexical
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.undo.IUndoEngine

interface IAnimScrollViewModule {
    val selectedGroups : List<GroupNode>
    fun setGroup(group : GroupNode, on: Boolean)
    fun toggle(anim: FixedFrameAnimation) : Boolean

    var toggleMode: Boolean
    fun step()

    //
    fun shift(offset: Int)
}

class AnimScrollViewModule(
    private val _context : IViewSystem,
    private val _undoEngine : IUndoEngine)
    : IAnimScrollViewModule
{
    private val _ffas : MutableSet<FixedFrameAnimation> = mutableSetOf()
    private val _selectedGroups : MutableSet<GroupNode> = mutableSetOf()
    private var _toggleAnchor: Int = -1

    override val selectedGroups: List<GroupNode> get() = _selectedGroups.toList()

    override fun setGroup(group: GroupNode, on: Boolean) {
        if( on)
            _selectedGroups.add(group)
        else
            _selectedGroups.remove(group)
    }

    override fun toggle(anim: FixedFrameAnimation): Boolean {
        if( _ffas.contains(anim)){
            _ffas.remove(anim)
            return false
        }
        else {
            _ffas.add(anim)
            return true
        }
    }

    override var toggleMode: Boolean
        get() = _toggleAnchor >= 0
        set(value) { _toggleAnchor = if( !value ) -1 else _context.view }

    override fun step() {
        if( toggleMode) {
            _context.view =
                if( _context.view == _toggleAnchor) (_toggleAnchor + 1) % _context.numActiveViews
                else _toggleAnchor
        }
        else {
            _context.view = (_context.view + 1 ) % _context.numActiveViews
        }
    }

    override fun shift(offset: Int) {
        val numNodesInContext = _selectedGroups
            .map { it.children.filterIsInstance<LayerNode>().count() }
            .min() ?: 0
        if( numNodesInContext == 0)
            return

        _undoEngine.doAsAggregateAction("AnimShift View by $offset") {
            for( group in _selectedGroups) {
                val layerNodes = group.children.filterIsInstance<LayerNode>().asReversed()
                //println(group.name + ":" + layerNodes.joinToString(",") { it.name })

                for (viewNum in (0 until _context.numActiveViews)) {
                    // Cycle Properties
                    val remap = (0 until numNodesInContext)
                        .associate { Pair( MathUtil.cycle(0, numNodesInContext, it + offset), _context.get(layerNodes[it], viewNum) ) }
                    for( i in 0 until numNodesInContext )
                        _context.set(layerNodes[i], remap[i]!!, viewNum)

                    // Updated Selected On
                    val cn = _context.getCurrentNode(viewNum)
                    val currentNodeIndex = if( cn == null) -1 else layerNodes.indexOf(cn)
                    if( currentNodeIndex != -1){
                        val indexOfSelected = MathUtil.cycle(0, numNodesInContext, currentNodeIndex + offset)
                        //println("view:$viewNum : $currentNodeIndex -> $indexOfSelected")
                        _context.setCurrentNode(viewNum, layerNodes[indexOfSelected])
                    }
                }

                _ffas.forEach { ffa ->
                    val lexLayers = ffa.layers
                        .filterIsInstance<FfaLayerLexical>()
                        .filter { it.groupLink == group  }

                    val chars  = layerNodes
                        .mapIndexed { index, layerNode ->
                            val isTheGuy = (0 until _context.numActiveViews)
                                .any {
                                    val viewState = _context.get(layerNode, it)
                                    viewState.isVisible && viewState.alpha == 1f
                                }
                            if( !isTheGuy)
                                null
                            else
                                DefaultAnimCharMap.getCharForIndex(index) ?: ' '
                        }
                        .filterNotNull()
                    val lex = String(chars.toCharArray())
                    lexLayers.forEach { it -> it.lexicon = lex }

                }

            }
        }

    }
}