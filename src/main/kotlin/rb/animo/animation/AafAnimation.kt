package rb.animo.animation

import rb.animo.DrawContract
import rb.animo.io.aaf.AafColisionMapper
import rb.animo.io.aaf.AafFile
import rb.glow.gl.GLImage
import rb.vectrix.intersect.CollisionMultiObj
import rb.vectrix.intersect.CollisionObject
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ImmutableTransformD
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.floor
import rb.vectrix.shapes.Rect
import rb.vectrix.shapes.RectI

class AafStructure(
        val animations: List<AafAnimStructure>
)
{
    companion object{
        fun fromFile(file: AafFile) : AafStructure {
            return AafStructure( file.animations.map { anim-> AafAnimStructure(
                name = anim.name,
                originX = anim.ox,
                originY = anim.oy,
                frames = anim.frames.map { frame-> AafFrame(
                    chunks = frame.chunks.map { chunk -> AafChunk(
                        celRect = file.cels[chunk.celId].run { RectI(x, y, w, h) },
                        offsetX = chunk.offsetX,
                        offsetY = chunk.offsetY,
                        drawDepth = chunk.drawDepth,
                        idc = chunk.group ) },
                    hitbox = frame.hitboxes.map { hitbox -> AafHitbox(
                        hitbox.typeId,
                        AafColisionMapper.mapToVectrix(hitbox.col) ) }

                ) }
            ) })

        }
    }
}

class AafAnimStructure(
        val name: String,
        val frames: List<AafFrame>,
        val originX: Int = 0,
        val originY: Int = 0)
class AafFrame(
        val chunks: List<AafChunk>,
        val hitbox: List<AafHitbox> = emptyList())
class AafChunk(
        val celRect: RectI,
        val offsetX: Int,
        val offsetY: Int,
        val drawDepth: Int,
        val idc: Char = ' ')

class AafHitbox(
        val typeId: Int,
        val col: CollisionObject)

class AafAnimation(
        val structure: AafAnimStructure,
        val img : GLImage)
    :IAnimation
{
    override val length: Double get() = len.d
    val len = structure.frames.count()

    override fun draw(met: Double, properties: RenderProperties, depth: Int): Collection<DrawContract> {
        val localMet = ((met.floor % len) + len ) % len
        val frame = structure.frames[localMet]
        val chunks = frame.chunks

        val drawTrans =
                properties.trans *
                ImmutableTransformD.Scale(1.0, -1.0)*
                ImmutableTransformD.Translation(-structure.originX.d, -structure.originY.d)


        return chunks.map {
            DrawContract(-it.drawDepth - depth, properties.copy(trans = drawTrans)) { gc -> gc.renderImage(img, it.offsetX.d, it.offsetY.d, imgPart = it.celRect)}
        }

    }

    override fun getHitbox(met: Double, type: Int): CollisionObject? {
        val localMet = ((met.floor % len) + len ) % len
        val frame = structure.frames[localMet]
        val cols = frame.hitbox.filter { it.typeId == type }.map { it.col }

        return when {
            cols.isEmpty() -> null
            cols.size == 1 -> cols.single()
            else -> CollisionMultiObj(cols)
        }
    }

}