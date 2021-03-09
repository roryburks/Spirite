package spirite.base.file.aaf

import rb.animo.io.aaf.writer.AafWriterFactory
import rb.animo.io.aaf.writer.IAafWriterFactory
import rb.vectrix.mathUtil.CyclicRedundancyChecker
import rb.vectrix.mathUtil.IDataStreamHasher
import rb.vectrix.rectanglePacking.ModifiedSleatorAlgorithm
import rbJvm.animo.JvmWriter
import sguiSwing.hybrid.Hybrid
import sguiSwing.hybrid.IImageCreator
import sguiSwing.hybrid.IImageIO
import spirite.base.file.aaf.export.AafExportConverter
import spirite.base.file.aaf.export.IAafExportConverter
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
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
        private val imageCreator: IImageCreator,
        private  val imageExporter: IImageIO,
        private val hasher: IDataStreamHasher,
        private val _aafWriterFactory : IAafWriterFactory = AafWriterFactory )
    : IAafExporter
{
    private val _converter : IAafExportConverter = AafExportConverter(imageCreator, ModifiedSleatorAlgorithm)

    override fun export(animation: FixedFrameAnimation, filename: String) {
        // 0 : select file name
        val (pngFilename, aafFilename) = getFilenames(filename)

        // 1: Parse as data
        val (aaf, img) = _converter.convert2(animation)

        // 2: Save PNG
        imageExporter.saveImage(img, File(pngFilename))

        // 3: Save Aaf
        val file = File(aafFilename)
        if( file.exists())
            file.delete()
        file.createNewFile()
        val ra = RandomAccessFile(file, "rw")
        ra.use { ra ->
            val writer = JvmWriter(ra)
            val aafWriter = _aafWriterFactory.makeWriter(4)
            aafWriter.write(writer, aaf)
        }

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



}