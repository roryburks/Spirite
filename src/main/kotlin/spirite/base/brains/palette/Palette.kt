package spirite.base.brains.palette

import rb.glow.color.Color
import rb.glow.color.Colors
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

val default_palette = arrayOf(
        Colors.BLACK, Colors.DARK_GRAY, Colors.GRAY, Colors.LIGHT_GRAY, Colors.WHITE,
        Colors.RED, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW,
        Colors.ORANGE, Colors.PINK)

abstract class Palette( name: String, raw: ByteArray? = null) {
    abstract val onChangeTrigger: (Palette) -> Unit

    val colors : Map<Int, Color> get() = _colors
    private var _colors = mutableMapOf<Int, Color>()
    var name: String = name
        get
        set(value) {
            field = value
            onChangeTrigger.invoke(this)
        }

    // region Constructors
    init {

        if( raw == null) {
            default_palette
                    .mapIndexed { i, color -> Pair(i, color) }
                    .toMap(_colors)
        }
        else {
            val stream = ByteArrayInputStream(raw)

            var caret = 0
            var chuckSize = stream.read()

            while( stream.available() > 0) {
                if( chuckSize == 0) {
                    caret += stream.read()
                }
                else {
                    for( i in 0 until chuckSize) {
                        val r = stream.read()
                        val g = stream.read()
                        val b = stream.read()
                        val a = stream.read()
                        val c = Colors.toColor(a, r, g, b)
                        _colors.put( i+caret, c)
                    }
                    caret += chuckSize
                }
                chuckSize = stream.read()
            }
        }
    }
    // endregion

    fun setPaletteColor( i: Int, color: Color?) {
        when( color) {
            null -> _colors.remove(i)
            else -> _colors[i] = color
        }
        onChangeTrigger.invoke(this)
    }

    fun removePaletteColor( i : Int) {
        _colors.remove(i)
        onChangeTrigger.invoke(this)
    }

    fun addPaletteColor( color: Color) {
        if( !colors.containsValue( color)) {
            var i = 0
            while( colors.containsKey( i++));
            _colors[i] = color
            onChangeTrigger.invoke(this)
        }
    }

    fun compress() : ByteArray {
        // For the most part Palettes are stored as an array of 4 bytes per
        //	jcolor in RGBA format/order.  But to preserve dimensionality of
        //	the palette while avoiding excessive "whitespace" bytes, the following
        // 	format is used:

        // [1] First byte corresponds to number of consecutive jcolor datas
        // [4*n] n*4 bytes representing the jcolor data, in RGBA form
        //		(if first byte was 0x00),
        //		[1] next byte represents consecutive empty datas
        val stream = ByteArrayOutputStream()

        // Step 1: find the highest Color index
        val lastIndex = colors.entries.maxBy { it.key }?.key ?: 0

        // Step 2: itterate through, constructing raw data
        var caret = 0
        var peekCount = 0
        var data = false

        while( caret <= lastIndex) {
            data = colors.containsKey(caret)
            peekCount = 1

            while( colors.containsKey(caret + peekCount) == data)
                peekCount++

            while( peekCount > 0) {
                // Note since we're using bytes to denote distance, in the offchance
                // that there are more than 255 conescutives, make sure to plus
                //	intermediate markets
                val tCount = if(peekCount > 0xff) 0xff else peekCount

                if( !data) {
                    stream.write(0x00)
                    stream.write(tCount)
                }
                else {
                    stream.write(tCount)
                    for( i in 0 until tCount) {
                        val c = colors[caret+i]!!.argb32
                        stream.write(Colors.getRed(c))
                        stream.write(Colors.getGreen(c))
                        stream.write(Colors.getBlue(c))
                        stream.write(Colors.getAlpha(c))
                    }
                }

                peekCount -= tCount
                caret += tCount
            }
        }

        return stream.toByteArray()
    }

    override fun toString() = name
}