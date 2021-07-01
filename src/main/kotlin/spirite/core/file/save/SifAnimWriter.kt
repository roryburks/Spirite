package spirite.core.file.save

import rb.file.IWriteStream
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*

object SifAnimWriter {
    const val MaxFFALayers = Short.MAX_VALUE.toInt()
    const val MaxFFALayerFrames = Short.MAX_VALUE.toInt()

    fun write(out: IWriteStream, data: SifAnimChunk) {
        for (animation in data.animations) {
            out.writeStringUft8Nt(animation.name)
            out.writeFloat(animation.speed)
            out.writeShort(animation.zoom.i)
            when(animation.data) {
                is SifAnimAnim_FixedFrame -> {
                    out.writeByte(SifConstants.ANIM_FFA)

                    val layers = animation.data.layers.take(MaxFFALayers)
                    if( animation.data.layers.count() > MaxFFALayers)
                        println("Animation Layers Trimmed on Save")
                    out.writeShort(layers.size)
                    for (layer in layers) {
                        out.writeStringUft8Nt(layer.partTypeName)
                        out.writeByte(if( layer.isAsync) 1 else 0)

                        when( layer.data) {
                            is SifAnimFfaLayer_Grouped -> {
                                out.writeByte(SifConstants.FFALAYER_GROUPLINKED)
                                out.writeInt(layer.data.groupNodeId)
                                out.writeByte(if( layer.data.subgroupsLinked) 1 else 0)

                                val frames = layer.data.frames.take(MaxFFALayerFrames)
                                if( layer.data.frames.size > MaxFFALayerFrames)
                                    println("FFA Group Linked Frames trimmed on write")
                                out.writeShort(frames.size)
                                for (frame in frames) {
                                    out.writeByte(frame.type.i)
                                    out.writeInt(frame.nodeId)
                                    out.writeShort(frame.len)
                                }
                            }
                            is SifAnimFfaLayer_Lexical -> {
                                out.writeByte(SifConstants.FFALAYER_LEXICAL)
                                out.writeInt(layer.data.groupedNodeId)
                                out.writeStringUft8Nt(layer.data.lexicon)

                                val mappings = layer.data.explicitMapping.take(255)
                                out.writeByte(mappings.size)
                                for (mapping in mappings) {
                                    out.writeByte(mapping.first.toByte().toInt())
                                    out.writeByte(mapping.second)
                                }
                            }
                            is SifAnimFfaLayer_Cascading -> {
                                out.writeByte(SifConstants.FFALAYER_CASCADING)
                                out.writeInt(layer.data.groupedNodeId)
                                out.writeStringUft8Nt(layer.data.lexicon)

                                val sublayers = layer.data.sublayers.take(255)
                                out.writeByte(sublayers.size)
                                for( sublayer in sublayers) {
                                    out.writeInt(sublayer.nodeId)
                                    out.writeShort(sublayer.primaryLen)
                                    out.writeByte(sublayer.lexicalKey.toByte().toInt())
                                    out.writeStringUft8Nt(sublayer.lexicon)
                                }
                            }
                        }
                    }
                }
                else -> throw SifFileException("Can't wrote animation: ${animation.name}")
            }

        }

    }
}