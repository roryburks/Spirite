package spirite.base.file.aaf

import rb.extendo.extensions.pop
import rb.extendo.extensions.toLookup
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.CyclicRedundancyChecker
import rb.vectrix.mathUtil.IDataStreamHasher
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.mediums.IImageMedium
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.util.linear.Rect
import spirite.base.util.rectanglePacking.BottomUpPacker
import spirite.base.util.rectanglePacking.PackedRectangle
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
        val images = getAllImages(animation)

        // Step 2: Check to see which (if any) are exact duplicates, re-mapping them if they are
        val deduplicated = deDuplicateImages(images)

        // Step 3: Use Rectangle Packing Algorithm to pack them.
        val packed = BottomUpPacker.pack(
                deduplicated.flatMap { (0 until it.value.size).map { _ -> it.key } })

        // Step 4: Construct packed Image and map from Image -> Rect
        val (outputImg, imgMap) = drawAndMap(packed, deduplicated)

        // Step 5: Save Png
        imageExporter.saveImage(outputImg, File(pngFilename))

        // Step 6: Save Aaf
        val file = File(aafFilename)
        if( file.exists())
            file.delete()
        file.createNewFile()

        val ra = RandomAccessFile(file, "rw")
        AafFileSaver.saveAAF(ra, animation, imgMap)
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

    fun getAllImages(animation: FixedFrameAnimation) : Sequence<ShiftedImage> {
        return animation.layers.asSequence()
                .flatMap { it.frames.asSequence().filterIsInstance<FFAFrame>().map { it.node }.filterIsInstance<LayerNode>() }
                .flatMap { it.getDrawList().asSequence() }
                .map { it.handle.medium }
                .filterIsInstance<IImageMedium>()
                .flatMap { it.getImages().asSequence() }
    }

    fun deDuplicateImages(images: Sequence<ShiftedImage>) : Map<Vec2i,MutableList<ShiftedImage>>
    {
        val imagesByDimension = images.toLookup { Vec2i(it.image.width, it.image.height) }

        fun checkImagesIdentical(image1: IImage, image2: IImage) : Boolean {
            if( image1 == image2) return true
            if( image1.width != image2.width) return false
            if( image1.height != image2.height) return false

            for (x in 0 until image1.width) {
                for( y in 0 until image1.height) {
                    if( image1.getARGB(x,y) != image2.getARGB(x,y)) {
                        return false
                    }
                }
            }
            return true
        }
//        for( e in imagesByDimension) {
//            if( e.value.size <= 1) continue
//            val toRemove = mutableListOf<ShiftedImage>()
//            val byHash = e.value
//                    .toLookup { hasher.getHash(it.image.byteStream) }
//
//            for( hashed in  byHash) {
//                var knownUnique = 1
//                val list = hashed.value
//
//                while (list.size > knownUnique) {
//                    list
//                            .subList(knownUnique+1,list.lastIndex)
//                            .removeIf {check -> checkImagesIdentical(list[knownUnique].image, check.image)
//                                    .also { if( it) toRemove.add(check) }}
//                    knownUnique++
//                }
//            }
//            e.value.removeAll(toRemove)
//        }

        imagesByDimension.values.removeIf { it.none() }
        return imagesByDimension
    }

    internal data class ImageLink(
            val img: ShiftedImage,
            val rect: Rect,
            val id: Int)

    private fun drawAndMap(packed: PackedRectangle, images: Map<Vec2i, MutableList<ShiftedImage>>) : Pair<RawImage, List<ImageLink>>
    {
        val img = imageCreator.createImage(packed.width, packed.height)

        val map = packed.packedRects
                .mapIndexed { index, rect ->
                    val image = images[Vec2i(rect.width, rect.height)]!!.pop()
                    img.graphics.renderImage(image.image, rect.x, rect.y)
                    ImageLink(image, rect, index)
                }

        return Pair(img,map)
    }
}