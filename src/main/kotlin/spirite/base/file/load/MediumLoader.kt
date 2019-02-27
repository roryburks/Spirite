package spirite.base.file.load

import rb.vectrix.mathUtil.i
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.file.SaveLoadUtil
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
        SaveLoadUtil.MEDIUM_MAGLEV -> MagneticMediumPartialLoader
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

object MagneticMediumPartialLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra

        val numThings = ra.readUnsignedShort()
        val things = List<IMaglevThing?>(numThings) {
            val thingType = ra.readByte()
            when( thingType.i) {
                SaveLoadUtil.MAGLEV_THING_STROKE -> {
                    val color = ra.readInt().toColor()
                    val strokeMethod = StrokeParams.Method.fromFileId(ra.readByte().i) ?: BASIC
                    val strokeWidth = ra.readFloat()
                    val mode =
                            if( context.version < 0x1_0006) PenDrawMode.NORMAL
                            else PenDrawMode.fromFileId(ra.readUnsignedByte())

                    val numVertices = ra.readUnsignedShort()

                    val x = FloatArray(numVertices)
                    val y = FloatArray(numVertices)
                    val w = FloatArray(numVertices)

                    repeat(numVertices) { i ->
                        x[i] = ra.readFloat()
                        y[i] = ra.readFloat()
                        w[i] = ra.readFloat()
                    }

                    MaglevStroke(
                            StrokeParams(color, strokeMethod, width = strokeWidth, mode = mode),
                            DrawPoints(x,y,w))
                }
                SaveLoadUtil.MAGLEV_THING_FILL -> {
                    MDebug.handleWarning(UNSUPPORTED, "Maglev Fills are currently not supported by Spirite v2, ignoring.")
                    ra.readInt()
                    ra.readByte()
                    val numReferences = ra.readUnsignedShort()
                    repeat(numReferences) {
                        ra.readUnsignedShort()
                        ra.readFloat()
                        ra.readFloat()
                    }
                    null
                }
                else -> {
                    MDebug.handleError(FILE, "Unrecognized MaglevThing Type: ${thingType.i}")
                    null
                }

            }
        }.filterNotNull()

        return MaglevMedium(context.workspace, context.workspace.mediumRepository, things)
    }
}

object MagneticMediumIgnorer : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra
        MDebug.handleWarning(UNSUPPORTED, "Maglev Mediums are currently not supported by Spirite v2, ignoring.")
        val numThings = ra.readUnsignedShort()
        repeat(numThings) {
            val thingType = ra.readByte()
            when( thingType.i) {
                0 -> { // stroke
                    ra.readInt()
                    ra.readByte()
                    ra.readFloat()
                    val numVertices = ra.readUnsignedShort()
                    repeat(numVertices) {
                        ra.readFloat()
                        ra.readFloat()
                        ra.readFloat()
                    }
                }
                1 -> { // fill
                    ra.readInt()
                    ra.readByte()
                    val numReferences = ra.readUnsignedShort()
                    repeat(numReferences) {
                        ra.readUnsignedShort()
                        ra.readFloat()
                        ra.readFloat()
                    }
                }
            }
        }
        return null
    }
}