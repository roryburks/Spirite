package spirite.base.graphics.isolation

import rb.extendo.extensions.then
import rb.glow.gle.RenderRubric
import spirite.base.graphics.isolation.IsolationManager.SpriteIsolationStruct
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

internal class SpriteIsolator(
    private val map: HashMap<Pair<GroupNode,String?>, SpriteIsolationStruct>,
    val node: Node
) : ISpriteLayerIsolator
{
    override fun getIsolatorForNode(node: Node): IIsolator {
        return SpriteIsolator(map, node)
    }
    override fun getIsolationForPart(part: SpritePart): IIsolator {
        val ancestors = if(node is GroupNode) node.ancestors.then(node) else node.ancestors

        var isDrawn = true
        var alpha = 1f
        for (ancestor in ancestors) {
            val isolation = map[Pair(ancestor,part.partName)]

            if( isolation != null && (isolation.includeSubtree || ancestor == node.parent)) {
                if( !isolation.isDrawn) {
                    isDrawn = false
                    break
                }
                alpha *= isolation.alpha
            }

            val invertIsolation = map[Pair(ancestor,null)]
            if( invertIsolation != null &&
                    (invertIsolation.includeSubtree || ancestor == node.parent) &&
                    (invertIsolation.partName != part.partName))
            {
                if( !invertIsolation.isDrawn) {
                    isDrawn = false
                    break
                }
                alpha *= invertIsolation.alpha
            }
        }

        return when {
            !isDrawn -> NilNodeIsolator
            alpha == 1f -> TrivialNodeIsolator
            else -> ExplicitIsolator(RenderRubric(alpha = alpha))
        }
    }

    override val isDrawn: Boolean get() = true
    override val rubric: RenderRubric? get() = null
}



class SingleNodeIsolator( private val nodeToIsolate: Node) : IIsolator {
    override fun getIsolatorForNode(node: Node) = when(node) {
        nodeToIsolate -> TrivialNodeIsolator
        else -> this
    }

    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? get() = null
}

object TrivialNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = true
    override val rubric: RenderRubric? = null
}
class ExplicitIsolator(override val rubric: RenderRubric) : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = true
}
