package spirite.base.file

import com.jogamp.opengl.util.packrect.RectanglePacker
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.mediums.IImageMedium
import spirite.base.imageData.mediums.IImageMedium.ShiftedImage
import spirite.base.util.*
import spirite.base.util.groupExtensions.toLookup
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec2i
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
        val packed = modifiedSleatorAlgorithm(
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
        saveAAF(ra, animation, imgMap)
    }

    val regex = Regex("""\.([^.\\\/]+)${'$'}""")
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
        val list1 = animation.layers.flatMap { it.frames.map { it.node }.filterIsInstance<LayerNode>() }
        val list2 = list1.flatMap { it.getDrawList() }
        val list3 = list2.map { it.handle.medium }
        val list4 = list3.filterIsInstance<IImageMedium>()
        val list5 = list4.flatMap { it.getImages() }
        return list5.asSequence()
    } /*animation.layers.asSequence()
            .flatMap { it.frames.asSequence().filterIsInstance<LayerNode>() }
            .flatMap { it.getDrawList().asSequence() }
            .map { it.handle.medium }
            .filterIsInstance<IImageMedium>()
            .flatMap { it.getImages().asSequence() }*/

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
        for( e in imagesByDimension) {
            if( e.value.size <= 1) continue
            val toRemove = mutableListOf<ShiftedImage>()
            val byHash = e.value
                    .toLookup { hasher.getHash(it.image.byteStream) }

            for( hashed in  byHash) {
                var knownUnique = 1
                val list = hashed.value

                while (list.size > knownUnique) {
                    list
                            .subList(knownUnique+1,list.lastIndex)
                            .removeIf {check -> checkImagesIdentical(list[knownUnique].image, check.image)
                                    .also { if( it) toRemove.add(check) }}
                    knownUnique++
                }
            }
            e.value.removeAll(toRemove)
        }

        imagesByDimension.values.removeIf { it.none() }
        return imagesByDimension
    }

    private data class ImageLink(
            val img: ShiftedImage,
            val rect: Rect,
            val id: Int)

    private fun drawAndMap( packed: PackedRectangle, images: Map<Vec2i, MutableList<ShiftedImage>>) : Pair<RawImage, List<ImageLink>>
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

    private fun saveAAF(ra: RandomAccessFile, animation: FixedFrameAnimation, imgMap: List<ImageLink>)
    {
        // [4]: Header
        ra.writeInt(2)

        ra.writeShort(1) // [2] : Num Anims
        ra.writeUFT8NT(animation.name)  // [n] : Animation Name


        val imgIdByImage = imgMap
                .map { Pair(it.img, it.id) }
                .toMap()

        val len = animation.end - animation.start
        ra.writeShort(len)    // [2] : Number of Layers
        for( met in animation.start until animation.end) {
            val things = animation.getDrawList(met.f).asSequence()
                    .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
                    .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
                    .toList()

            ra.writeShort(things.size)  // [2] : Number of Chunks
            for( (img,transformed) in things) {
                ra.writeShort(imgIdByImage[img]!!)  // [2]: ChunkId
                ra.writeShort(transformed.renderRubric.transform.m02.round + img.x) // [2] OffsetX
                ra.writeShort(transformed.renderRubric.transform.m12.round + img.y) // [2] OffsetY
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