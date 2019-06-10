package spirite.base.file.aaf

import rb.extendo.extensions.pop
import rb.extendo.extensions.toLookup
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.CyclicRedundancyChecker
import rb.vectrix.mathUtil.IDataStreamHasher
import rb.vectrix.rectanglePacking.ModifiedSleatorAlgorithm
import rb.vectrix.rectanglePacking.PackedRectangle
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.mediums.IImageMedium
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.util.linear.Rect
import spirite.hybrid.Hybrid
import spirite.hybrid.IImageCreator
import spirite.hybrid.IImageIO
import java.io.File
import java.io.RandomAccessFile

interface IAafExporter {
    fun export( animation: FixedFrameAnimation, filename:String)
}

val defaultAafExporter : IAafExporter by lazy {
    AafExporter(Hybrid.imageCreator,
            Hybrid.imageIO,
            CyclicRedundancyChecker())
}

class AafExporter(
        val imageCreator: IImageCreator,
        val imageExporter: IImageIO,
        val hasher: IDataStreamHasher)
    : IAafExporter
{
    override fun export(animation: FixedFrameAnimation, filename: String) {
        val (pngFilename, aafFilename) = getFilenames(filename)

        // Step 1: Gather all flat image segments needed by the animation
        val uniqueImages = getAllImages(animation)
                .toList()

        // Step 3: Use Rectangle Packing Algorithm to pack them.
        val packed = ModifiedSleatorAlgorithm(
                uniqueImages.map { Vec2i(it.width, it.height) })

        // Step 3: Construct packed Image and map from Image -> Rect
        val aafInfo = drawAndMap(packed, uniqueImages)

        // Step 4: Save Png
        imageExporter.saveImage(aafInfo.img, File(pngFilename))

        // Step 5: Save Aaf
        val file = File(aafFilename)
        if( file.exists())
            file.delete()
        file.createNewFile()

        val ra = RandomAccessFile(file, "rw")
        AafFileSaver.saveAAF(ra, animation, aafInfo)
    }

    val regex = Regex("""\.([^.\\/]+)${'$'}""")
    fun getFilenames(filename: String) : Pair<String, String>
    {
        val extension = regex.find(filename)?.groupValues?.getOrNull(1)

        return when(extension) {
            "png" -> Pair(filename, filename.substring(0,filename.length - 3) + "aaf")
            "aaf" ->  Pair(filename.substring(0,filename.length - 3) + "png", filename)
            null -> Pair("$filename.png", "$filename.aaf")
            else -> Pair(filename.substring(0,filename.length - extension.length) + "png", filename)
        }
    }

    fun getAllImages(animation: FixedFrameAnimation) : Sequence<IImage> {
        return animation.layers.asSequence()
                .flatMap { it.frames.asSequence().filterIsInstance<FFAFrame>().map { it.node }.filterIsInstance<LayerNode>() }
                .flatMap { it.getDrawList().asSequence() }
                .map { it.handle.medium }
                .filterIsInstance<IImageMedium>()
                .flatMap { it.getImages().asSequence() }
                .map { it.image }
                .distinct()
    }

//    fun deDuplicateImages(images: Sequence<IImage>) : Map<IImage,MutableList<IImage>>
//    {
//        val imagesByDimension = images.toLookup { Vec2i(it.image.width, it.image.height) }
//
//        val existingImages = hashSetOf<IImage>()
//
//        fun checkImagesIdentical(image1: IImage, image2: IImage) : Boolean {
//            if( image1 == image2) return true
//            if( image1.width != image2.width) return false
//            if( image1.height != image2.height) return false
//
//            for (x in 0 until image1.width) {
//                for( y in 0 until image1.height) {
//                    if( image1.getARGB(x,y) != image2.getARGB(x,y)) {
//                        return false
//                    }
//                }
//            }
//            return true
//        }
////        for( e in imagesByDimension) {
////            if( e.value.size <= 1) continue
////            val toRemove = mutableListOf<ShiftedImage>()
////            val byHash = e.value
////                    .toLookup { hasher.getHash(it.image.byteStream) }
////
////            for( hashed in  byHash) {
////                var knownUnique = 1
////                val list = hashed.value
////
////                while (list.size > knownUnique) {
////                    list
////                            .subList(knownUnique+1,list.lastIndex)
////                            .removeIf {check -> checkImagesIdentical(list[knownUnique].image, check.image)
////                                    .also { if( it) toRemove.add(check) }}
////                    knownUnique++
////                }
////            }
////            e.value.removeAll(toRemove)
////        }
//
//        imagesByDimension.values.removeIf { it.none() }
//        return imagesByDimension
//    }


    internal data class AafInfo(
            val img: RawImage,
            val chunks: List<Rect>,
            val chunkMap: Map<IImage,Int>)

    private fun drawAndMap(packed: PackedRectangle, uniqueImages: List<IImage>)
            : AafInfo
    {
        val img = imageCreator.createImage(packed.width, packed.height)

        val imagesByDimension = uniqueImages.toLookup { Vec2i(it.width, it.height) }

        val chunkMap = packed.packedRects
                .mapIndexed { index, rect ->
                    val image= imagesByDimension[Vec2i(rect.wi, rect.hi)]!!.pop()
                    img.graphics.renderImage(image, rect.x1i, rect.y1i)
                    Pair(image, index)
                }.toMap()

        val chunks = packed.packedRects.map { Rect(it.x1i, it.y1i,it.wi, it.hi) }

        return AafInfo(img, chunks, chunkMap)
    }
}