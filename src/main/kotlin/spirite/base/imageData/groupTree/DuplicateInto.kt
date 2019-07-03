package spirite.base.imageData.groupTree

import rb.extendo.dataStructures.Deque
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.StringUtil

fun MovableGroupTree.duplicateInto(toDupe: Node, context: Node = selectedNode ?: root) {
    undoEngine?.doAsAggregateAction("Duplicate Node Into") {
        val nodeMappings = workspace.paletteMediumMap.getNodeMappings()
        val spriteMappings = workspace.paletteMediumMap.getSpriteMappings()
        fun insertAndDupeProperties(toInsert: Node, duping: Node, contextNode: Node?, noRecBlock: Boolean) {
            toInsert.x = duping.x
            toInsert.y = duping.y
            toInsert.alpha = duping.alpha
            toInsert.method = duping.method
            toInsert.visible = duping.visible
            toInsert.expanded = duping.expanded

            nodeMappings[duping]?.also { workspace.paletteMediumMap.set(toDupe, it)}
            if( toInsert is GroupNode) {
                spriteMappings.entries.filter { it.key.first == duping }
                        .forEach { workspace.paletteMediumMap.set(toInsert, it.key.second, it.value) }
            }
            insertNode(contextNode, toInsert, noRecBlock)
        }

        fun getNonduplicateName(name: String) = StringUtil.getNonDuplicateName(root.getAllNodesSuchThat({true}).map { it.name }.toSet(), name)

        when (toDupe) {
            is LayerNode -> {
                val dupedLayer = toDupe.layer.dupe(workspace)
                val dupedNode = this.LayerNode(null, toDupe.name, dupedLayer)
                insertAndDupeProperties(dupedNode, toDupe, context, false)
            }
            is GroupNode -> {
                data class NodeContext(val toDupe: Node, val parentInDuper: GroupNode, val noRecBlock : Boolean= false)

                val dupeQ = Deque<NodeContext>()
                val dupeRoot = addGroupNode(context, getNonduplicateName(toDupe.name), true)

                toDupe.children.forEach { dupeQ.addBack(NodeContext(it, dupeRoot)) }

                while (true) {
                    val next = dupeQ.popFront() ?: break

                    val toInsert = when (next.toDupe) {
                        is GroupNode -> this.GroupNode(null, getNonduplicateName(next.toDupe.name))
                                .apply { next.toDupe.children.forEach { dupeQ.addBack(NodeContext(it, this)) } }
                        is LayerNode -> this.LayerNode(null, getNonduplicateName(next.toDupe.name), next.toDupe.layer.dupe(workspace))
                        else -> null
                    } ?: continue

                    insertAndDupeProperties(toInsert, next.toDupe, next.parentInDuper, next.noRecBlock)
                }
            }
            else -> throw NotImplementedError("Unrecognized Node Type When duping into")
        }
    }
}