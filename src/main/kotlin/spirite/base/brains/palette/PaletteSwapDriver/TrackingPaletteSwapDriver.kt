package spirite.base.brains.palette.PaletteSwapDriver

import spirite.base.brains.palette.PaletteBelt
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Color
import kotlin.math.min

object TrackingPaletteSwapDriver
    :IPaletteSwapDriver
{
    private val _spritePartMap : MutableMap<Pair<GroupNode,String>,List<Color>> = mutableMapOf()
    private val _nodeMap : MutableMap<Node,List<Color>> = mutableMapOf()

    private var _oldSpritePart : Pair<GroupNode, SpritePart>? = null
    private var _oldNode : Node? = null

    override fun onMediumChenge(mediumHandle: MediumHandle) {
        rememberAndForget(mediumHandle)

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
                    val mapped = _spritePartMap[Pair(spriteContext, activePart.partName)]
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

    private fun rememberAndForget(mediumHandle: MediumHandle)
    {
        val belt = mediumHandle.workspace.paletteManager.activeBelt

        val oldNode = _oldNode
        if( oldNode != null)
            _nodeMap[oldNode] = (0 until belt.size).map { belt.getColor(it) }

        _oldNode = null

        val oldSpritePart = _oldSpritePart
        if( oldSpritePart != null)
            _spritePartMap[Pair(oldSpritePart.first, oldSpritePart.second.partName)] = (0 until belt.size).map { belt.getColor(it) }
        _oldSpritePart = null

    }

}