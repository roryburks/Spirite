package rb.animo.io.aaf.util

import rb.animo.io.aaf.AafFile
import rb.extendo.extensions.toLookup

object AafUtil {
    fun deepCompare( aaf1: AafFile, aaf2: AafFile) : String? {
        val anims1ByName = aaf1.animations.toLookup { it.name }

        for (anim2 in aaf2.animations) {
            val anim1 = anims1ByName[anim2.name]?.firstOrNull() ?: return "Anim1 is missing ${anim2.name}"

            if( anim1.ox != anim2.ox) return "Anim Ox Mismatch"
            if( anim1.oy != anim2.oy) return "Anim Oy Mismatch"

            if( anim1.frames.size != anim2.frames.size) return  "Anim Frame Size Mismatch"

            for( i in anim1.frames.indices) {
                val frame1 = anim1.frames[i]
                val frame2 = anim2.frames[i]
                if( frame1.chunks.size != frame2.chunks.size) return "Chunk Size Mismatch"
                if( frame1.hitboxes.size != frame2.hitboxes.size) return "Hitbox Size Mismatch"

                for( j in frame1.chunks.indices) {
                    val chunk1 = frame1.chunks[j]
                    val chunk2 = frame2.chunks[j]
                    val cel1 = aaf1.cels[chunk1.celId]
                    val cel2 = aaf2.cels[chunk2.celId]
                    if( cel1.x != cel2.x || cel1.y != cel2.y || cel1.w != cel2.w || cel1.h != cel2.h) return "Cel detail mismatch"
                    if( chunk1.drawDepth != chunk2.drawDepth) return "Chunk DrawDepth Mismatch"
                    if( chunk1.group != chunk2.group) return "Chunk Group Mismatch"
                    if( chunk1.offsetX != chunk2.offsetX) return "Chunk Ox mismatch"
                    if( chunk1.offsetY != chunk2.offsetY) return "Chunk OY mismatch"
                }

                // ignoring collision deep comparing for now
            }
        }

        return null
    }
}