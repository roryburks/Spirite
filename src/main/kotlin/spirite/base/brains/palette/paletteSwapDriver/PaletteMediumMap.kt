package spirite.base.brains.palette.paletteSwapDriver

import rb.glow.color.Color
import rb.owl.bindable.addObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import kotlin.math.min

interface IPaletteMediumMap
{
    fun getNodeMappings() : Map<Node,List<Color>>
    fun getSpriteMappings() : Map<Pair<GroupNode,String>,List<Color>>

    fun import(
            nodeMappings: Map<Node,List<Color>>,
            spriteMappings: Map<Pair<GroupNode,String>, List<Color>>)
}

class PaletteMediumMap(private val _workspace: IImageWorkspace)
    :IPaletteMediumMap
{
    private var _nodeMap = mutableMapOf<Node,List<Color>>()
    private var _spriteMap = mutableMapOf<Pair<GroupNode,String>,List<Color>>()

    private var _oldSpritePart : Pair<GroupNode, SpritePart>? = null
    private var _oldNode : Node? = null

    // region IPaletteMediumMap
    override fun getNodeMappings() : Map<Node,List<Color>>{
        forgetAllUnlinked()
        return _nodeMap
    }
    override fun getSpriteMappings() : Map<Pair<GroupNode,String>,List<Color>> {
        forgetAllUnlinked()
        return _spriteMap
    }

    override fun import(nodeMappings: Map<Node, List<Color>>, spriteMappings: Map<Pair<GroupNode, String>, List<Color>>) {
        _nodeMap = nodeMappings.toMutableMap()
        _spriteMap = spriteMappings.toMutableMap()
    }
    // endregion

    // region Internal Methods
    private fun onMediumChange(mediumHandle: MediumHandle) {
        rememberAndForget()

        val belt = mediumHandle.workspace.paletteManager.activeBelt
        val selectedNode = mediumHandle.workspace.groupTree.selectedNode

        if( selectedNode != null)
        {
            val spriteLayer = (selectedNode as? LayerNode)?.layer as? SpriteLayer
            val spriteContext = selectedNode.parent
            if( spriteLayer != null && spriteContext != null)
            {
                val activePart = spriteLayer.activePart
                if( activePart != null) {
                    _oldSpritePart = Pair(spriteContext, activePart)
                    val mapped = _spriteMap[Pair(spriteContext, activePart.partName)]
                    if( mapped != null)
                        (0 until min(mapped.size, belt.size)).forEach { belt.setColor(it, mapped[it]) }
                }
            }
            else
            {
                _oldNode = selectedNode
                val mapped = _nodeMap[selectedNode]
                if( mapped != null)
                    (0 until min(mapped.size, belt.size)).forEach { belt.setColor(it, mapped[it]) }

            }
        }

    }

    private fun forgetAllUnlinked() {
        val root = _workspace.groupTree.root
        _nodeMap.entries.removeIf{!it.key.ancestors.contains(root)}
    }

    private fun rememberAndForget() {
        val belt = _workspace.paletteManager.activeBelt

        val oldNode = _oldNode
        if( oldNode != null)
            _nodeMap[oldNode] = (0 until belt.size).map { belt.getColor(it) }

        _oldNode = null

        val oldSpritePart = _oldSpritePart
        if( oldSpritePart != null)
            _spriteMap[Pair(oldSpritePart.first, oldSpritePart.second.partName)] = (0 until belt.size).map { belt.getColor(it) }
        _oldSpritePart = null
    }
    // endregion

    // region Bindings
    private val _activeMediumK = _workspace.activeMediumBind.addObserver { new, _ ->
        if( new != null && _workspace.paletteManager.drivePalette) {
            onMediumChange(new)
        }
    }
    // endregion
}