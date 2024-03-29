package spirite.base.file.sif.v1.load

import rb.file.BufferedReadStream
import rb.glow.ColorARGB32Normal
import rb.glow.toColor
import rb.vectrix.interpolation.CubicSplineInterpolator2D
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.round
import rbJvm.file.JvmRandomAccessFileBinaryReadStream
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.file.readFloatArray
import spirite.base.file.sif.SaveLoadUtil
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevFill.StrokeSegment
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.pen.PenState
import spirite.base.pen.stroke.BasicDynamics
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.DrawPointsBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import spirite.base.pen.stroke.StrokeParams.Method.BASIC
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug.ErrorType.FILE
import spirite.core.hybrid.IDebug.WarningType.UNSUPPORTED
import spirite.sguiHybrid.Hybrid


object MagneticMediumLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra

        context.tel2.start("MegMediumLoad1")
        val numThings = ra.readUnsignedShort()


        val things = List<IMaglevThing?>(numThings) {
                val thingType = ra.readByte()
                when (thingType.i) {
                    SaveLoadUtil.MAGLEV_THING_STROKE -> {
                        val color = ra.readInt().toColor()
                        val strokeMethod = Method.fromFileId(ra.readByte().i) ?: BASIC
                        val strokeWidth = ra.readFloat()
                        val mode = PenDrawMode.fromFileId(ra.readUnsignedByte()) ?: PenDrawMode.NORMAL

                        val numVertices = ra.readInt()

                        context.tel2.start("MegMediumLoad_Stroke_ArrayRead")
                        val x = ra.readFloatArray(numVertices)
                        val y = ra.readFloatArray(numVertices)
                        val w = ra.readFloatArray(numVertices)
                        context.tel2.end("MegMediumLoad_Stroke_ArrayRead")

                        context.tel2.start("MegMediumLoad_Stroke_Convert")
                        val stroke = MaglevStroke(
                                StrokeParams(color, strokeMethod, width = strokeWidth, mode = mode),
                                DrawPoints(x, y, w))
                        context.tel2.end("MegMediumLoad_Stroke_Convert")
                        stroke
                    }
                    SaveLoadUtil.MAGLEV_THING_FILL -> {
                        context.tel2.start("MegMediumLoad_Stroke_Fill")
                        val color = ColorARGB32Normal(ra.readInt())
                        val mode = MagneticFillMode.fromFileId(ra.readByte().i)!!

                        val numSeqments = ra.readUnsignedShort()
                        val segments = List(numSeqments) {
                            val strokeId = ra.readInt()
                            val start = ra.readInt()
                            val end = ra.readInt()
                            StrokeSegment(strokeId, start, end)
                        }
                        val fill = MaglevFill(segments, mode, color)
                        context.tel2.end("MegMediumLoad_Stroke_Fill")
                        fill
                    }
                    else -> {
                        DebugProvider.debug.handleError(FILE, "Unrecognized MaglevThing Type: ${thingType.i}")
                        null
                    }

                }
            }.filterNotNull()
        context.tel2.end("MegMediumLoad1")
        context.tel2.start("MegMediumLoad2")

        val imgSize = if( context.version >= 0x0001_0009) ra.readInt() else 0
        val xoffset = if( context.version >= 0x0001_0009) ra.readUnsignedShort() else 0
        val yoffset = if( context.version >= 0x0001_0009) ra.readUnsignedShort() else 0

        val img = when( imgSize) {
            0 -> null
            else -> {
                val imgData = ByteArray(imgSize).apply { ra.read( this) }
                Hybrid.imageIO.loadImage(imgData)
            }
        }

        val thingMap = things.mapIndexed { i, thing -> Pair(i,thing) }.toMap()
        val ret = MaglevMedium(context.workspace, thingMap, DynamicImage(img, xoffset, yoffset), (thingMap.keys.max() ?: 0)+1)
        context.tel2.end("MegMediumLoad2")
        return ret
    }
}

object MagneticMediumLoader_V2 : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        //val ra = context.ra
        val ra = BufferedReadStream(JvmRandomAccessFileBinaryReadStream(context.ra))

        context.tel2.start("MegMediumLoad1")
        val numThings = ra.readUnsignedShort()


        val things = List<IMaglevThing?>(numThings) {
            val thingType = ra.readByte()
            when (thingType.i) {
                SaveLoadUtil.MAGLEV_THING_STROKE -> {
                    val color = ra.readInt().toColor()
                    val strokeMethod = Method.fromFileId(ra.readByte().i) ?: BASIC
                    val strokeWidth = ra.readFloat()
                    val mode = PenDrawMode.fromFileId(ra.readUnsignedByte()) ?: PenDrawMode.NORMAL

                    val numVertices = ra.readInt()

                    context.tel2.start("MegMediumLoad_Stroke_ArrayRead")
                    val x = ra.readFloatArray(numVertices)
                    val y = ra.readFloatArray(numVertices)
                    val w = ra.readFloatArray(numVertices)
                    context.tel2.end("MegMediumLoad_Stroke_ArrayRead")

                    context.tel2.start("MegMediumLoad_Stroke_Convert")
                    val stroke = MaglevStroke(
                            StrokeParams(color, strokeMethod, width = strokeWidth, mode = mode),
                            DrawPoints(x, y, w))
                    context.tel2.end("MegMediumLoad_Stroke_Convert")
                    stroke
                }
                SaveLoadUtil.MAGLEV_THING_FILL -> {
                    context.tel2.start("MegMediumLoad_Stroke_Fill")
                    val color = ColorARGB32Normal(ra.readInt())
                    val mode = MagneticFillMode.fromFileId(ra.readByte().i)!!

                    val numSeqments = ra.readUnsignedShort()
                    val segments = List(numSeqments) {
                        val strokeId = ra.readInt()
                        val start = ra.readInt()
                        val end = ra.readInt()
                        StrokeSegment(strokeId, start, end)
                    }
                    val fill = MaglevFill(segments, mode, color)
                    context.tel2.end("MegMediumLoad_Stroke_Fill")
                    fill
                }
                else -> {
                    DebugProvider.debug.handleError(FILE, "Unrecognized MaglevThing Type: ${thingType.i}")
                    null
                }

            }
        }.filterNotNull()
        context.tel2.end("MegMediumLoad1")
        context.tel2.start("MegMediumLoad2")

        val imgSize = if( context.version >= 0x0001_0009) ra.readInt() else 0
        val xoffset = if( context.version >= 0x0001_0009) ra.readUnsignedShort() else 0
        val yoffset = if( context.version >= 0x0001_0009) ra.readUnsignedShort() else 0

        val img = when( imgSize) {
            0 -> null
            else -> {
                val imgData = ra.readByteArray(imgSize)
                //val imgData = ByteArray(imgSize).apply { ra.read( this) }
                Hybrid.imageIO.loadImage(imgData)
            }
        }

        val thingMap = things.mapIndexed { i, thing -> Pair(i,thing) }.toMap()
        val ret = MaglevMedium(context.workspace, thingMap, DynamicImage(img, xoffset, yoffset), (thingMap.keys.max() ?: 0)+1)
        context.tel2.end("MegMediumLoad2")
        if( ra._buffer != null) {
            val rollback = (ra._bufferValidSize - ra._bCarat)
            context.ra.seek(context.ra.filePointer - rollback)
        }
        return ret
    }
}


// region Legacy
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
                    val strokeMethod = Method.fromFileId(ra.readByte().i) ?: BASIC
                    val strokeWidth = ra.readFloat()
                    val mode =
                            if( context.version < 0x1_0006) PenDrawMode.NORMAL
                            else PenDrawMode.fromFileId(ra.readUnsignedByte()) ?: PenDrawMode.NORMAL

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
                    DebugProvider.debug.handleWarning(UNSUPPORTED, "Maglev Fills should not show up in version x1_0000 - x1_0007.  Do not know how to interpret (trying to ignore in old style)")
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
                    DebugProvider.debug.handleError(FILE, "Unrecognized MaglevThing Type: ${thingType.i}")
                    null
                }

            }
        }.filterNotNull()

        val thingMap = things.mapIndexed { i, thing -> Pair(i,thing) }.toMap()
        return MaglevMedium(context.workspace, thingMap)
    }
}

object Legacy_pre_1_0000_MaglevMediumLoader : IMediumLoader {
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra
        val numThings = ra.readUnsignedShort()
        val things = List(numThings) {
            val thingType = ra.readByte()
            val strokeLengths = mutableListOf<Int?>()
            val thing: IMaglevThing = when( thingType.i) {
                SaveLoadUtil.MAGLEV_THING_STROKE -> {
                    // Note: converts pen state based points into built draw points based points
                    val color = ColorARGB32Normal(ra.readInt())
                    val method  = Method.fromFileId(ra.readByte().i) ?: throw BadSifFileException("Unrecognized Method.")
                    val strokeWidth = ra.readFloat()

                    val numVertices = ra.readUnsignedShort()
                    val penStates = List<PenState>(numVertices) {
                        val x = ra.readFloat()
                        val y = ra.readFloat()
                        val w = ra.readFloat()
                        PenState(x, y, w)
                    }

                    val params = StrokeParams(color = color, method =  method, width = strokeWidth)
                    val points = DrawPointsBuilder.buildPoints(CubicSplineInterpolator2D(), penStates, BasicDynamics)
                    MaglevStroke(params, points)
                }
                SaveLoadUtil.MAGLEV_THING_FILL -> {
                    // Note: converts [0,1] float-basd indexing to built  draw points based indexing
                    val color = ColorARGB32Normal(ra.readInt())
                    val mode = MagneticFillMode.fromFileId(ra.readByte().i) ?: throw BadSifFileException("Unrecognized Mode.")

                    val numSegments = ra.readUnsignedShort()
                    val segments = List<StrokeSegment>(numSegments) {
                        val strokeId = ra.readUnsignedShort()
                        val startF = ra.readFloat()
                        val endF = ra.readFloat()

                        val strokeSize = strokeLengths.getOrNull(strokeId) ?: 0

                        val start = MathUtil.clip ( 0, (strokeSize* startF).round, strokeSize-1)
                        val end = MathUtil.clip ( 0, (strokeSize* endF).round, strokeSize-1)
                        StrokeSegment(strokeId, start, end)
                    }

                    MaglevFill(segments, mode, color)
                }
                else -> throw BadSifFileException("Unrecognized Thing type in Legacy_pre_1_0000_MaglevMediumLoader: ${thingType.i}")
            }
            strokeLengths.add( (thing as? MaglevStroke)?.drawPoints?.length)
            thing
        }
        val thingMap = things.mapIndexed { i, thing -> Pair(i,thing) }.toMap()
        return MaglevMedium(context.workspace, thingMap)
    }
}

object Legacy_pre_1_000_MagneticMediumIgnorer : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra
        DebugProvider.debug.handleWarning(UNSUPPORTED, "Maglev Mediums are currently not supported by Spirite v2, ignoring.")
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
// endregion