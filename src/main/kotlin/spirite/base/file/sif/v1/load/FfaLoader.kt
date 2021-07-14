package spirite.base.file.sif.v1.load

import rb.vectrix.mathUtil.i
import spirite.sguiHybrid.MDebug
import spirite.sguiHybrid.MDebug.WarningType.STRUCTURAL
import spirite.sguiHybrid.MDebug.WarningType.UNSUPPORTED
import spirite.base.file.sif.SaveLoadUtil
import spirite.base.file.readUTF8NT
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.base.imageData.animation.ffa.FfaFrameStructure
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FfaLayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node


object FfaLoader : IAnimationLoader {
    override fun loadAnimation(context: LoadContext, name: String): FixedFrameAnimation {
        val ra = context.ra
        val ffa = FixedFrameAnimation(name, context.workspace)

        val numLayers = ra.readUnsignedShort()
        repeat(numLayers){_->
            val layerName : String =
                    if( context.version <= 0x1_000A ) "FFA Layer"
                    else ra.readUTF8NT()
            val asynchronous : Boolean =
                    if( context.version <= 0x1_000A ) false
                    else ra.readByte().i == 1

            val layerType =
                    if( context.version <= 0x1_0003) SaveLoadUtil.FFALAYER_GROUPLINKED
                    else ra.readUnsignedByte()

            val layerLoader = when(layerType) {
                SaveLoadUtil.FFALAYER_GROUPLINKED -> FfaFixedGroupLayerLoader
                SaveLoadUtil.FFALAYER_LEXICAL -> FfaLexicalLayerLoader
                SaveLoadUtil.FFALAYER_CASCADING -> FfaCascadingLayerLoader
                else -> null
            }
            if( layerLoader == null) MDebug.handleWarning( UNSUPPORTED,"Unknown FFA Layer Type: $layerType.  Attempting to skip, but likely Corrupting")

            else layerLoader.load(context, ffa, layerName)?.
                    also {  it.asynchronous = asynchronous }
        }
        return ffa
    }
}

interface IFfaLayerLoader {
    fun load(context: LoadContext, ffa: FixedFrameAnimation, name: String) : IFfaLayer?

}

object FfaCascadingLayerLoader : IFfaLayerLoader {
    override fun load(context: LoadContext, ffa: FixedFrameAnimation, name: String): IFfaLayer? {
        val ra = context.ra
        val nodes = context.nodes

        val group = nodes.getOrNull(ra.readInt()) as? GroupNode
        val lexicon = ra.readUTF8NT()

        val subinfoCount = ra.readUnsignedByte()
        val subinfos = (0 until subinfoCount).mapNotNull {
            val infoGroup= nodes.getOrNull(ra.readInt()) as? GroupNode
            val plen = ra.readUnsignedShort()
            val key = ra.readByte().toChar()
            val subLexicon = if( context.version < 0x0001_000D) "" else ra.readUTF8NT()

            if( infoGroup == null) null
            else FfaCascadingSublayerContract(infoGroup, key, plen, if( subLexicon == "") null else subLexicon)
        }

        return if( group == null) null
            else ffa.addCascadingLayer(group,name, subinfos, lexicon)
    }
}

object FfaLexicalLayerLoader : IFfaLayerLoader {
    override fun load(context: LoadContext, ffa: FixedFrameAnimation, name: String) : IFfaLayer? {
        val ra = context.ra
        val nodes = context.nodes

        val groupId = ra.readInt()
        val group = nodes.getOrNull(groupId) as? GroupNode
        val lexicon = ra.readUTF8NT()

        val explicitCount = ra.readUnsignedByte()
        val map =
                if( explicitCount == 0) null
                else (0..explicitCount).asSequence()
                        .map { Pair(ra.readByte().toChar(), nodes[ra.readInt()]) }
                        .toMap()

        if( group != null)
            return ffa.addLexicalLayer(group, name, lexicon, map)
        return null
    }
}

object FfaFixedGroupLayerLoader : IFfaLayerLoader {
    override fun load(context: LoadContext, ffa: FixedFrameAnimation, name: String) : IFfaLayer? {
        val ra = context.ra
        val nodes = context.nodes
        val node = nodes.getOrNull(ra.readInt())
        val includeSubtrees = (ra.readByte().i != 0)

        val numFrames = ra.readUnsignedShort()

        val frameMap = mutableMapOf<Node, FfaFrameStructure>()
        val unlinkedFrameClusters = mutableListOf<UnlinkedFrameCluster>()

        var workingNode : Node? = null
        var workingUnlinkedFrames = mutableListOf<FfaFrameStructure>()

        repeat(numFrames) {
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
                workingUnlinkedFrames.add(FfaFrameStructure(null, marker, length))
            }
            else {
                frameMap[frameNode] = FfaFrameStructure(frameNode, marker, length)
                if (workingUnlinkedFrames.any()) unlinkedFrameClusters.add(UnlinkedFrameCluster(workingNode, workingUnlinkedFrames))
                workingUnlinkedFrames = mutableListOf()
                workingNode = frameNode
            }
        }


        return when(val groupNode = node as? GroupNode) {
            null -> {
                MDebug.handleWarning(STRUCTURAL, "FFA Layer has a non-Group GroupNode marked as its Link")
                null
            }
            else -> ffa.addLinkedLayer(groupNode, includeSubtrees, name, frameMap, unlinkedFrameClusters)
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
            val nodeMap = mutableMapOf<Node, FfaFrameStructure>()

            repeat(framecount) {
                val marker = ra.readByte().i
                val length = ra.readShort()
                val nodeLink = if( marker == 0) ra.readInt() else 0
                if( nodeLink > 0) {
                    val node = context.nodes[nodeLink]
                    nodeMap[node] = FfaFrameStructure(node, FRAME, length.i)
                }
            }

            (groupNode as? GroupNode)?.also { t-> ffa.addLinkedLayer(t, false, frameMap = nodeMap) }
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
                            is LayerNode -> Pair(frameNode, FfaFrameStructure(frameNode, FRAME, innerLength))
                            is GroupNode -> Pair(frameNode, FfaFrameStructure(frameNode, START_LOCAL_LOOP, innerLength))
                            else -> null
                        }
                    }
                    .toMap()

            val unlinkedFrameClusters = indexToGapMap.map{entry ->
                UnlinkedFrameCluster(nodeList.getOrNull(entry.key), List(entry.value) { FfaFrameStructure(null, GAP, 1) })
            }


            when(val groupNode = node as? GroupNode) {
                null -> MDebug.handleWarning(STRUCTURAL, "FFA Layer has a non-Group GroupNode marked as its Link")
                else -> ffa.addLinkedLayer(groupNode, includeSubtrees, frameMap = frameMap, unlinkedClusters = unlinkedFrameClusters)
            }
        }
        return ffa
    }
}
// endregion