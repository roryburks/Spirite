package spirite.base.file.aaf.export

import rb.animo.animation.AafAnimStructure
import rb.animo.animation.AafChunk
import rb.animo.animation.AafFrame
import rb.animo.animation.AafStructure
import rb.animo.io.aaf.*
import rb.extendo.extensions.pop
import rb.extendo.extensions.toLookup
import rb.glow.img.IImage
import rb.glow.img.RawImage
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import rb.vectrix.rectanglePacking.IRectanglePackingAlgorithm
import rb.vectrix.rectanglePacking.PackedRectangle
import rb.vectrix.shapes.RectI
import sgui.core.systems.IImageCreator
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.mediums.IImageMedium

data class AafFileMapping(
        val img: RawImage,
        val chunks: List<RectI>,
        val chunkMap: Map<IImage,Int>)

interface IAafExportConverter  {
    fun convert( ffa: FixedFrameAnimation) : Pair<AafStructure, AafFileMapping>
    fun convert2( ffa: FixedFrameAnimation) : Pair<AafFile, RawImage>
}

class AafExportConverter(
    private val _imageCreator: IImageCreator,
    private val _rpa : IRectanglePackingAlgorithm
)  : IAafExportConverter {
    override fun convert(ffa: FixedFrameAnimation): Pair<AafStructure,AafFileMapping> {
        val mmap = MediumNameMapper.getMap(ffa.workspace)

        // Step 1: Gather all flat image segments needed by the animation
        val uniqueImages = getAllImages(ffa).toList()

        // Step 2: Rectangle Packing
        val packed = _rpa.pack(uniqueImages.map { Vec2i(it.width, it.height) })

        // Step 3: Construct Packed Image and map from Image -> Rect
        val aafMapping = drawAndMap(packed, uniqueImages)

        val frames = (ffa.start until ffa.end).map { i ->
            val things = ffa.getDrawList(i.f).asSequence()
                    .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
                    .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
                    .toList()

            val chunks = things.map { (simg, transformed) ->
                val chunk = aafMapping.chunks[aafMapping.chunkMap[simg.image]!!]
                val partName = mmap[transformed.handle.id] ?: ""
                val cid = partName.getOrElse(0) {' '}
                AafChunk(
                        chunk,
                        transformed.renderRubric.transform.m02f.round + simg.x,
                        transformed.renderRubric.transform.m12f.round + simg.y,
                        transformed.drawDepth,
                        cid)
            }

            AafFrame(chunks)
        }

        val anim = AafAnimStructure(
                ffa.name,
                frames,
                0, 0 )
        return Pair(
                AafStructure(listOf(anim)),
                aafMapping)
    }

    override fun convert2(ffa: FixedFrameAnimation): Pair<AafFile, RawImage> {
        val mmap = MediumNameMapper.getMap(ffa.workspace)

        // Step 1: Gather all flat image segments needed by the animation
        val uniqueImages = getAllImages(ffa).toList()

        // Step 2: Rectangle Packing
        val packed = _rpa.pack(uniqueImages.map { Vec2i(it.width, it.height) })

        // Step 3: Construct Packed Image and map from Image -> Rect
        val aafMapping = drawAndMap(packed, uniqueImages)

        val frames = (ffa.start until ffa.end).map { i ->
            val things = ffa.getDrawList(i.f).asSequence()
                .mapNotNull { Pair(it, it.handle.medium as? IImageMedium ?: return@mapNotNull null) }
                .flatMap { (a,b) -> b.getImages().asSequence().map { Pair(it, a) } }
                .toList()

            val chunks = things.map { (simg, transformed) ->
                val chunkId = aafMapping.chunkMap[simg.image]!!
                val partName = mmap[transformed.handle.id] ?: ""
                val cid = partName.getOrElse(0) {' '}
                AafFChunk(
                    cid,
                    chunkId,
                    transformed.renderRubric.transform.m02f.round + simg.x,
                    transformed.renderRubric.transform.m12f.round + simg.y,
                    transformed.drawDepth)
            }

            AafFFrame(chunks, listOf())
        }

        val anim = AafFAnimation( ffa.name, ox = 0, oy = 0, frames = frames)
        val file = AafFile(
            -1,
            listOf(anim),
            aafMapping.chunks.map { AafFCel(it.x1i, it.y1i, it.wi, it.hi) } )
        return Pair( file, aafMapping.img)
    }

    fun getAllImages(animation: FixedFrameAnimation) : Sequence<IImage> {
        return animation.layers.asSequence()
                .flatMap { it.frames.asSequence().filterIsInstance<FFALayer.FFAFrame>().map { it.node }.filterIsInstance<GroupTree.LayerNode>() }
                .flatMap { it.getDrawList().asSequence() }
                .map { it.handle.medium }
                .filterIsInstance<IImageMedium>()
                .flatMap { it.getImages().asSequence() }
                .map { it.image }
                .distinct()
    }

    fun drawAndMap(packed: PackedRectangle, uniqueImages: List<IImage>)
            : AafFileMapping
    {
        val img = _imageCreator.createImage(packed.width, packed.height)

        val imagesByDimension = uniqueImages.toLookup { Vec2i(it.width, it.height) }

        val chunkMap = packed.packedRects
                .mapIndexed { index, rect ->
                    val image= imagesByDimension[Vec2i(rect.wi, rect.hi)]!!.pop()
                    img.graphics.renderImage(image, rect.x1, rect.y1)
                    Pair(image, index)
                }.toMap()

        val chunks = packed.packedRects.map { RectI(it.x1i, it.y1i,it.wi, it.hi) }

        return AafFileMapping(img, chunks, chunkMap)
    }
}