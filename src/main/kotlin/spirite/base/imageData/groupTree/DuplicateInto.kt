package spirite.base.imageData.groupTree

import rb.extendo.dataStructures.Deque
import spirite.base.imageData.groupTree.PrimaryGroupTree.InsertBehavior.Bellow
import spirite.core.util.StringUtil

fun MovableGroupTree.duplicateInto(toDupe: Node, context: Node = selectedNode ?: root) {
    undoEngine?.doAsAggregateAction("Duplicate Node Into") {
        val nodeMappings = workspace.paletteMediumMap.getNodeMappings()
        val spriteMappings = workspace.paletteMediumMap.getSpriteMappings()
        fun insertAndDupeProperties(toInsert: Node, duping: Node, contextNode: Node?) {
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
            insertNode(contextNode, toInsert)
        }

        fun getNonduplicateName(name: String) = StringUtil.getNonDuplicateName(root.getAllNodesSuchThat({true}).map { it.name }.toSet(), name)

        when (toDupe) {
            is LayerNode -> {
                val dupedLayer = toDupe.layer.dupe(workspace)
                val dupedNode = this.makeLayerNode(null, toDupe.name, dupedLayer)
                insertAndDupeProperties(dupedNode, toDupe, context)
            }
            is GroupNode -> {
                data class NodeContext(val toDupe: Node, val parentInDuper: GroupNode)

                val dupeQ = Deque<NodeContext>()
                val dupeRoot = addGroupNode(context, getNonduplicateName(toDupe.name), Bellow)

                toDupe.children.forEach { dupeQ.addBack(NodeContext(it, dupeRoot)) }

                while (true) {
                    val next = dupeQ.popFront() ?: break

                    val toInsert = when (next.toDupe) {
                        is GroupNode -> this.makeGroupNode(null, getNonduplicateName(next.toDupe.name))
                                .apply { next.toDupe.children.forEach { dupeQ.addBack(NodeContext(it, this)) } }
                        is LayerNode -> this.makeLayerNode(null, getNonduplicateName(next.toDupe.name), next.toDupe.layer.dupe(workspace))
                        else -> null
                    } ?: continue

                    insertAndDupeProperties(toInsert, next.toDupe, next.parentInDuper)
                }
            }
            else -> throw NotImplementedError("Unrecognized Node Type When duping into")
        }
    }
}