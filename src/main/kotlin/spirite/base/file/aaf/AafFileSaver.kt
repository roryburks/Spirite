package spirite.base.file.aaf

import spirite.base.file.aaf.AafExporter.ImageLink
import spirite.base.file.writeUFT8NT
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.mediums.IImageMedium
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import java.io.RandomAccessFile

object AafFileSaver {

    internal fun saveAAF(ra: RandomAccessFile, animation: FixedFrameAnimation, imgMap: List<ImageLink>)
    {
        // [4]: Header
        ra.writeInt(2)

        ra.writeShort(1) // [2] : Num Anims
        ra.writeUFT8NT(animation.name)  // [n] : Animation Name


        val imgIdByImage = imgMap
                .map { Pair(it.img, it.id) }
                .toMap()

        val len = animation.end - animation.start
        ra.writeShort(len)    // [2] : Number of Frames
        for( met in animation.start until animation.end) {
            val things = animation.getDrawList(met.f).asSequence()
                    .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
                    .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
                    .toList()

            ra.writeShort(things.size)  // [2] : Number of Chunks
            for( (img,transformed) in things) {
                ra.writeShort(imgIdByImage[img]!!)  // [2]: ChunkId
                ra.writeShort(transformed.renderRubric.transform.m02f.round + img.x) // [2] OffsetX
                ra.writeShort(transformed.renderRubric.transform.m12f.round + img.y) // [2] OffsetY
                ra.writeInt(transformed.drawDepth)  // [4] : DrawDepth
            }
        }

        ra.writeShort(imgMap.size)  // [2]: Num ImgChunks
        for (link in imgMap) {
            ra.writeShort(link.rect.x)
            ra.writeShort(link.rect.y)
            ra.writeShort(link.rect.width)
            ra.writeShort(link.rect.height)

        }
    }
}