package spirite.base.file

import com.jogamp.opengl.util.packrect.RectanglePacker
import spirite.base.graphics.IImage
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.mediums.IImageMedium
import spirite.base.util.CyclicRedundancyChecker
import spirite.base.util.groupExtensions.toLookup
import spirite.base.util.linear.Vec2i
import spirite.base.util.modifiedSleatorAlgorithm

interface IAafExporter {
    fun export( animation: FixedFrameAnimation, filename:String)
}

object AafExporter : IAafExporter {
    val hasher = CyclicRedundancyChecker()

    override fun export(animation: FixedFrameAnimation, filename: String) {
        // Step 1: Gather all flat image segments needed by the animation
        val imagesByDimension = animation.layers.asSequence()
                .flatMap { it.frames.asSequence().filterIsInstance<LayerNode>() }
                .flatMap { it.getDrawList().asSequence() }
                .filterIsInstance<IImageMedium>()
                .flatMap { it.getImages().asSequence() }
                .toLookup { Vec2i(it.image.width, it.image.height) }

        // Step 1.5: Check to see which (if any) are exact duplicates, re-mapping them if they are
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
            val byHash = e.value
                    .toLookup { hasher.getHash(it.image.byteStream) }

            for( hashed in  byHash) {
                var knownUnique = 1
                val list = hashed.value

                while (list.size > knownUnique) {
                    list
                            .subList(knownUnique+1,list.lastIndex)
                            .removeIf {checkImagesIdentical(list[knownUnique].image, it.image)}
                }
            }
        }





//        val packed = modifiedSleatorAlgorithm(images.map { Vec2i(it.image.width, it.image.height) }.toList())




        // Step 2: Use Rectangle Packing Algorithm to pack them.
        // Step 2.5: Save packed to

        // Step 3: Go through the animation frame by frame and export to AAF File

    }
}