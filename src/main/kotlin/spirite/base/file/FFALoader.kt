package spirite.base.file

import spirite.base.imageData.animation.ffa.FFAFrameStructure
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.i
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

interface IFFAAnimationLoader {
    fun loadAnimation(context: LoadContext, name: String) : FixedFrameAnimation
}

object FFALoaderFactory {
    fun getLoaderFromVersion( version: Int) : IFFAAnimationLoader = when( version) {
        in 0..0x1_0000 -> LegacyFFALoader_X_TO_1_0000()
        else -> FFALoader()
    }
}

class LegacyFFALoader_X_TO_1_0000 : IFFAAnimationLoader {
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

class FFALoader : IFFAAnimationLoader {
    override fun loadAnimation(context: LoadContext, name: String): FixedFrameAnimation {
        val ra = context.ra
        val nodes = context.nodes
        val ffa = FixedFrameAnimation(name, context.workspace)

        val numLayers = ra.readUnsignedShort()
        repeat(numLayers){_->
            val node = nodes[ra.readInt()]
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
        return ffa
    }
}