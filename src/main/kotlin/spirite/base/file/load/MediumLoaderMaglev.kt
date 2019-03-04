package spirite.base.file.load

import rb.vectrix.mathUtil.i
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.file.SaveLoadUtil
import spirite.base.file.readFloatArray
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.BASIC
import spirite.base.util.toColor
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FILE
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED


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
                    val mode = PenDrawMode.fromFileId(ra.readUnsignedByte())

                    val numVertices = ra.readInt()

                    val x = ra.readFloatArray(numVertices)
                    val y = ra.readFloatArray(numVertices)
                    val w = ra.readFloatArray(numVertices)

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

object Legacy_1_0006_MagneticMediumPartialLoader : IMediumLoader
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