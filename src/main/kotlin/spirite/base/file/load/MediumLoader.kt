package spirite.base.file.load

import rb.vectrix.mathUtil.i
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.file.SaveLoadUtil
import spirite.base.file.readFloatArray
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.BASIC
import spirite.base.util.toColor
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FILE
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED
import java.nio.ByteBuffer

interface IMediumLoader
{
    fun loadMedium(context : LoadContext) : IMedium?
}

object MediumLoaderFactory
{
    fun getMediumLoader( version: Int, typeId: Int) : IMediumLoader = when(typeId)
    {
        SaveLoadUtil.MEDIUM_PLAIN -> FlatMediumLoader
        SaveLoadUtil.MEDIUM_DYNAMIC -> DynamicMediumLoader
        SaveLoadUtil.MEDIUM_PRISMATIC -> PrismaticMediumIgnorer
        SaveLoadUtil.MEDIUM_MAGLEV ->
            if( version <= 0x1_0006) Legacy_1_0006_MagneticMediumPartialLoader
            else MagneticMediumPartialLoader
        else -> throw BadSifFileException("Unrecognized Medium Type Id: $typeId.  Trying to load a newer SIF version in an older program version or corrupt file.")
    }
}

object FlatMediumLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val imgSize = context.ra.readInt()
        val imgData = ByteArray(imgSize).apply { context.ra.read( this) }

        val img = Hybrid.imageIO.loadImage(imgData)
        return FlatMedium(img, context.workspace.mediumRepository)
    }
}

object DynamicMediumLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra
        val ox = ra.readShort().i
        val oy = ra.readShort().i
        val imgSize = ra.readInt()
        val img = when( imgSize) {
            0 -> null
            else -> {
                val imgData = ByteArray(imgSize).apply { ra.read( this) }
                Hybrid.imageIO.loadImage(imgData)
            }
        }
        return DynamicMedium(context.workspace, DynamicImage(img, ox, oy), context.workspace.mediumRepository)
    }
}

object PrismaticMediumIgnorer :IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra

        MDebug.handleWarning(UNSUPPORTED, "Prismatic Mediums are currently not supported by Spirite v2, ignoring.")
        val numlayers = ra.readUnsignedShort()
        repeat(numlayers) {
            ra.readInt()
            ra.readShort()
            ra.readShort()
            val imgSize = ra.readInt()
            ra.skipBytes(imgSize)
        }
        return null
    }
}