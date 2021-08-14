package spirite.core.file.load.anim

import rb.file.IReadStream
import rb.file.readStringUtf8
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*

class FfaReader(val version: Int): ISifAnimAnimationReader {
    override fun read(read: IReadStream): SifAnimAnimData {
        val numLayers = read.readUnsignedShort()
        val layers = List(numLayers) {
            val layerName = if( version <= 0x1_000A) "FFA Layer" else read.readStringUtf8()
            val async = if( version <= 0x1_000A) false else (read.readByte().i == 1)
            val layerType = if( version <= 0x1_0003)  SifConstants.FFALAYER_GROUPLINKED else read.readUnsignedByte()

            val layerData = when( layerType) {
                SifConstants.FFALAYER_GROUPLINKED -> loadGroupLinkedLayer(read)
                SifConstants.FFALAYER_LEXICAL -> loadLexicalLayer(read)
                SifConstants.FFALAYER_CASCADING -> loadCascadingLayer(read)
                else -> throw SifFileException("Unrecognized FFA Layer TypeId: $layerType")
            }

            SifAnimFfaLayer(layerName, async, layerData)
        }

        return SifAnimAnim_FixedFrame(layers)
    }

    fun loadGroupLinkedLayer(read: IReadStream) : SifAnimFfaLayer_Grouped {
        val groupNodeId = read.readInt()
        val includeSubtrees = (read.readUnsignedByte() != 0)
        val numFrames = read.readUnsignedShort()
        val frames = List(numFrames) {
            val frameType = read.readByte()
            val nodeId = read.readInt()
            val length = read.readUnsignedShort()


            SifAnimFfaLayer_Grouped.Frame(frameType, nodeId, length)
        }
        return SifAnimFfaLayer_Grouped(groupNodeId, includeSubtrees, frames)
    }

    fun loadLexicalLayer( read: IReadStream) : SifAnimFfaLayer_Lexical {
        val groupId = read.readInt()
        val lexicon = read.readStringUtf8()
        val explicitMappingCount = read.readUnsignedByte()
        val explicitMapping = List(explicitMappingCount) {
            val char = read.readByte().toChar()
            val nodeId = read.readInt()
            Pair( char, nodeId)
        }
        return SifAnimFfaLayer_Lexical(groupId, lexicon, explicitMapping)
    }

    fun loadCascadingLayer( read: IReadStream) : SifAnimFfaLayer_Cascading {
        val groupId = read.readInt()
        val lexicon = read.readStringUtf8()
        val subLayerCount = read.readUnsignedByte()
        val subLayers = List(subLayerCount) {
            val nodeId = read.readInt()
            val plen = read.readUnsignedShort()
            val key = read.readByte(). toChar()
            val subLex = if( version < 0x1_000D) "" else read.readStringUtf8()

            SifAnimFfaLayer_Cascading.SubLayer(nodeId, plen, key, subLex)
        }
        return SifAnimFfaLayer_Cascading(groupId, lexicon, subLayers)
    }
}

object LegacyFfaReader_X_to_7 : ISifAnimAnimationReader {
    override fun read(read: IReadStream): SifAnimAnimData {
        val layerCount = read.readUnsignedShort()

        val layers = List(layerCount) {
            val groupNodeId = read.readInt()
            val frameCount = read.readUnsignedShort()
            val frames = List(frameCount) {
                val marker = read.readByte().i
                val length = read.readShort().i

                when(marker) {
                    0 -> {
                        val nodeLink = read.readInt()
                        SifAnimFfaLayer_Grouped.Frame(SifConstants.FfaFrameMarker_Frame.toByte(), nodeLink, length)
                    }
                    else -> SifAnimFfaLayer_Grouped.Frame(SifConstants.FfaFrameMarker_Gap.toByte(), -1, length)
                }
            }

            val data = SifAnimFfaLayer_Grouped( groupNodeId, false, frames )
            SifAnimFfaLayer("Layer$it", false, data)
        }

        return SifAnimAnim_FixedFrame( layers )
    }
}


object LegacyFFAReader_8_TO_1_0000 : ISifAnimAnimationReader {
    override fun read(read: IReadStream): SifAnimAnimData {
        val layerCount = read.readUnsignedShort()
        val layers = List(layerCount) {
            val groupNodeId = read.readInt()
            val includeSubtrees = read.readByte().i != 0

            val numFrames = read.readUnsignedShort()
            val frames = mutableListOf<SifAnimFfaLayer_Grouped.Frame>()
            repeat(numFrames) {
                val frameNodeId = read.readInt()
                val innerLength = read.readUnsignedShort()
                val gapBefore = read.readUnsignedShort()
                val gapAfter = read.readUnsignedShort()

                if( gapBefore > 0)
                    frames.add(SifAnimFfaLayer_Grouped.Frame(SifConstants.FfaFrameMarker_Gap.toByte(), -1, gapBefore))

                frames.add(SifAnimFfaLayer_Grouped.Frame(SifConstants.FfaFrameMarker_Frame.toByte(), frameNodeId, innerLength))

                if( gapAfter > 0)
                    frames.add(SifAnimFfaLayer_Grouped.Frame(SifConstants.FfaFrameMarker_Gap.toByte(), -1, gapAfter))
            }

            val data = SifAnimFfaLayer_Grouped(groupNodeId, includeSubtrees, frames)
            SifAnimFfaLayer("Layer$it", false, data)
        }

        return SifAnimAnim_FixedFrame( layers )
    }
}