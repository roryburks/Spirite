package spirite.base.file.load

import spirite.base.file.SaveLoadUtil
import spirite.base.file.readNullTerminatedStringUTF8
import spirite.base.imageData.animation.ffa.FFAFrameStructure
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import rb.vectrix.mathUtil.i
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED


object FFALoader : IAnimationLoader {
    override fun loadAnimation(context: LoadContext, name: String): FixedFrameAnimation {
        val ra = context.ra
        val nodes = context.nodes
        val ffa = FixedFrameAnimation(name, context.workspace)

        val numLayers = ra.readUnsignedShort()
        repeat(numLayers){_->
            val layerType =
                    if( context.version <= 0x1_0003) SaveLoadUtil.FFALAYER_GROUPLINKED
                    else ra.readUnsignedByte()

            val layerLoader = when(layerType) {
                SaveLoadUtil.FFALAYER_GROUPLINKED -> FFAFixedGroupLayerLoader
                SaveLoadUtil.FFALAYER_LEXICAL -> FFALexicalLayerLoader
                else -> null
            }
            layerLoader?.load(context, ffa) ?:MDebug.handleWarning( UNSUPPORTED,"Unknown FFA Layer Type: $layerType.  Attempting to skip, but likely Corrupting")
        }
        return ffa
    }
}

interface IFFALayerLoader {
    fun load(context: LoadContext, ffa: FixedFrameAnimation)

}

object FFALexicalLayerLoader : IFFALayerLoader {
    override fun load(context: LoadContext, ffa: FixedFrameAnimation) {
        val ra = context.ra
        val nodes = context.nodes

        val group = nodes.getOrNull(ra.readInt()) as? GroupNode
        val lexicon = ra.readNullTerminatedStringUTF8()

        val explicitCount = ra.readUnsignedByte()
        val map =
                if( explicitCount == 0) null
                else (0..explicitCount).asSequence()
                        .map { Pair(ra.readByte().toChar(), nodes[ra.readInt()]) }
                        .toMap()

        if( group != null)
            ffa.addLexicalLayer(group, lexicon, map)
    }
}

object FFAFixedGroupLayerLoader : IFFALayerLoader {
    override fun load(context: LoadContext, ffa: FixedFrameAnimation) {
        val ra = context.ra
        val nodes = context.nodes
        val node = nodes.getOrNull(ra.readInt())
        val includeSubtrees = (ra.readByte().i == 0)

        val numFrames = ra.readUnsignedShort()

        val frameMap = mutableMapOf<Node, FFAFrameStructure>()
        val unlinkedFrameClusters = mutableListOf<UnlinkedFrameCluster>()

        var workingNode : Node? = null
        var workingUnlinkedFrames = mutableListOf<FFAFrameStructure>()

        repeat(numFrames) {_->
            val frameType =  ra.readByte().i
            val frameNode = nodes.getOrNull(ra.readInt())
            val length = ra.readUnsignedShort()

            val marker = when(frameType) {
                SaveLoadUtil.FFAFRAME_STARTOFLOOP -> START_LOCAL_LOOP
                SaveLoadUtil.FFAFRAME_FRAME -> FRAME
                SaveLoadUtil.FFAFRAME_GAP -> GAP
                else -> {
                    MDebug.handleWarning(STRUCTURAL, "Unrecognized FFAFrame Type: $frameType")
                    GAP
                }
            }

            if( frameNode == null) {
                workingUnlinkedFrames.add(FFAFrameStructure(null, marker, length))
            }
            else {
                frameMap[frameNode] = FFAFrameStructure(frameNode, marker, length)
                if (workingUnlinkedFrames.any()) unlinkedFrameClusters.add(UnlinkedFrameCluster(workingNode, workingUnlinkedFrames))
                workingUnlinkedFrames = mutableListOf()
                workingNode = frameNode
            }
        }


        val groupNode = node as? GroupNode
        when( groupNode) {
            null -> MDebug.handleWarning(STRUCTURAL, "FFA Layer has a non-Group Node marked as its Link")
            else -> ffa.addLinkedLayer(groupNode, includeSubtrees, frameMap, unlinkedFrameClusters)
        }
    }

}

// region Legacy Loaders

object LegacyFFALoader_X_To_7 : IAnimationLoader {
    override fun loadAnimation(context: LoadContext, name: String): FixedFrameAnimation {
        val ra = context.ra
        val layerCount = ra.readUnsignedShort()
        val ffa = FixedFrameAnimation(name, context.workspace)

        repeat(layerCount) {
            val groupNodeId = ra.readInt()
            val framecount = ra.readUnsignedShort()

            val groupNode = if( groupNodeId == 0) null else context.nodes.getOrNull(groupNodeId)
            val nodeMap = mutableMapOf<Node, FFAFrameStructure>()

            repeat(framecount) {
                val marker = ra.readByte().i
                val length = ra.readShort()
                val nodeLink = if( marker == 0) ra.readInt() else 0
                if( nodeLink > 0) {
                    val node = context.nodes[nodeLink]
                    nodeMap[node] = FFAFrameStructure(node, FRAME, length.i)
                }
            }

            (groupNode as? GroupNode)?.also { t-> ffa.addLinkedLayer(t, false, nodeMap) }
        }

        return  ffa
    }

}

object LegacyFFALoader_8_TO_1_0000 : IAnimationLoader {
    override fun loadAnimation(context: LoadContext, name: String): FixedFrameAnimation {
        val ra = context.ra
        val nodes = context.nodes
        val ffa = FixedFrameAnimation(name, context.workspace)

        val numLayers = ra.readUnsignedShort()
        repeat(numLayers){_->
            val node = nodes[ra.readInt()]
            val includeSubtrees = (ra.readByte().i == 0)

            val numFrames = ra.readUnsignedShort()

            val indexToGapMap = mutableMapOf<Int,Int>()
            val nodeList = mutableListOf<Node?>()
            val frameMap = (0 until numFrames)
                    .mapNotNull {
                        val frameNode = nodes.getOrNull(ra.readInt())
                        val innerLength = ra.readUnsignedShort()
                        val gapBefore = ra.readUnsignedShort()
                        val gapAfter = ra.readUnsignedShort()

                        nodeList.add(frameNode)
                        if( gapBefore != 0) {
                            indexToGapMap[it-1] = gapBefore
                        }
                        if( gapAfter != 0) {
                            indexToGapMap[it] = (indexToGapMap[it]?:0) + gapAfter
                        }

                        when( frameNode) {
                            is LayerNode -> Pair(frameNode, FFAFrameStructure(frameNode, FRAME, innerLength))
                            is GroupNode -> Pair(frameNode, FFAFrameStructure(frameNode, START_LOCAL_LOOP, innerLength))
                            else -> null
                        }
                    }
                    .toMap()

            val unlinkedFrameClusters = indexToGapMap.map{entry ->
                UnlinkedFrameCluster(nodeList.getOrNull(entry.key), List(entry.value) { FFAFrameStructure(null, GAP, 1) })
            }


            val groupNode = node as? GroupNode
            when( groupNode) {
                null -> MDebug.handleWarning(STRUCTURAL, "FFA Layer has a non-Group Node marked as its Link")
                else -> ffa.addLinkedLayer(groupNode, includeSubtrees, frameMap, unlinkedFrameClusters)
            }
        }
        return ffa
    }
}
// endregion