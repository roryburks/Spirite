//package spirite.base.file.aaf
//
//import rb.vectrix.mathUtil.f
//import rb.vectrix.mathUtil.round
//import spirite.base.file.aaf.AafExporter.AafFileMapping
//import spirite.base.file.aaf.export.MediumNameMapper
//import spirite.base.file.writeUFT8NT
//import spirite.base.imageData.IImageWorkspace
//import spirite.base.imageData.animation.ffa.FixedFrameAnimation
//import spirite.base.imageData.groupTree.GroupTree
//import spirite.base.imageData.layers.sprite.SpriteLayer
//import spirite.base.imageData.mediums.IImageMedium
//import java.io.RandomAccessFile
//
//interface IAafFileSaver {
//    fun saveAAF(ra: RandomAccessFile, animation: FixedFrameAnimation, aafInfo: AafFileMapping)
//}
//
//object AafFileSaver_3 : IAafFileSaver {
//    override fun saveAAF(ra: RandomAccessFile, animation: FixedFrameAnimation, aafInfo: AafFileMapping)
//    {
//        val mmap = MediumNameMapper.getMap( animation.workspace)
//
//        // [4]: Header
//        ra.writeInt(3)
//
//        ra.writeShort(1) // [2] : Num Anims
//        ra.writeUFT8NT(animation.name)  // [n] : Animation Name
//
//
//        val len = animation.end - animation.start
//        ra.writeShort(len)    // [2] : Number of Frames
//        for( met in animation.start until animation.end) {
//            val things = animation.getDrawList(met.f).asSequence()
//                    .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
//                    .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
//                    .toList()
//
//            ra.writeShort(things.size)  // [2] : Number of Chunks
//            for( (img,transformed) in things) {
//                val name = mmap[transformed.handle.id] ?: ""
//                val char = name.getOrElse(0) {' '}
//                ra.writeByte(char.toInt())
//                ra.writeShort(aafInfo.chunkMap[img.image]!!)  // [2]: ChunkId
//                ra.writeShort(transformed.renderRubric.transform.m02f.round + img.x) // [2] OffsetX
//                ra.writeShort(transformed.renderRubric.transform.m12f.round + img.y) // [2] OffsetY
//                ra.writeInt(transformed.drawDepth)  // [4] : DrawDepth
//            }
//        }
//
//        ra.writeShort(aafInfo.chunks.size)  // [2]: Num ImgChunks
//        for (chunk in aafInfo.chunks) {
//            ra.writeShort(chunk.x)
//            ra.writeShort(chunk.y)
//            ra.writeShort(chunk.width)
//            ra.writeShort(chunk.height)
//
//        }
//    }
//}
//object AafFileSaver_2 {
//
//    internal fun saveAAF(ra: RandomAccessFile, animation: FixedFrameAnimation, aafInfo: AafFileMapping)
//    {
//        // [4]: Header
//        ra.writeInt(2)
//
//        ra.writeShort(1) // [2] : Num Anims
//        ra.writeUFT8NT(animation.name)  // [n] : Animation Name
//
//
//        val len = animation.end - animation.start
//        ra.writeShort(len)    // [2] : Number of Frames
//        for( met in animation.start until animation.end) {
//            val things = animation.getDrawList(met.f).asSequence()
//                    .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
//                    .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
//                    .toList()
//
//            ra.writeShort(things.size)  // [2] : Number of Chunks
//            for( (img,transformed) in things) {
//                ra.writeShort(aafInfo.chunkMap[img.image]!!)  // [2]: ChunkId
//                ra.writeShort(transformed.renderRubric.transform.m02f.round + img.x) // [2] OffsetX
//                ra.writeShort(transformed.renderRubric.transform.m12f.round + img.y) // [2] OffsetY
//                ra.writeInt(transformed.drawDepth)  // [4] : DrawDepth
//            }
//        }
//
//        ra.writeShort(aafInfo.chunks.size)  // [2]: Num ImgChunks
//        for (chunk in aafInfo.chunks) {
//            ra.writeShort(chunk.x)
//            ra.writeShort(chunk.y)
//            ra.writeShort(chunk.width)
//            ra.writeShort(chunk.height)
//
//        }
//    }
//}