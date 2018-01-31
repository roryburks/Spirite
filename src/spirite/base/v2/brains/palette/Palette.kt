package spirite.base.v2.brains.palette

import spirite.base.util.Colors
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private val default_palette = arrayOf(
        Colors.BLACK, Colors.DARK_GRAY, Colors.GRAY, Colors.LIGHT_GRAY, Colors.WHITE,
        Colors.RED, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW,
        Colors.ORANGE, Colors.PINK)

abstract class Palette( name: String, raw: ByteArray? = null) {
    abstract val onChangeTrigger: (Palette) -> Unit

    val colors : Map<Int,Int> get() = _colors
    private var _colors = mutableMapOf<Int,Int>()
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

    fun setPaletteColor( i: Int, color: Int) {
        _colors[i] = color
        onChangeTrigger.invoke(this)
    }

    fun removePaletteColor( i : Int) {
        _colors.remove(i)
        onChangeTrigger.invoke(this)
    }

    fun addPaletteColor( color: Int) {
        if( !colors.containsValue( color)) {
            var i = 0
            while( colors.containsKey( i++));
            _colors[i] = color
            onChangeTrigger.invoke(this)
        }
    }

    fun compress() : ByteArray {
        // For the most part Palettes are stored as an array of 4 bytes per
        //	color in RGBA format/order.  But to preserve dimensionality of
        //	the palette while avoiding excessive "whitespace" bytes, the following
        // 	format is used:

        // [1] First byte corresponds to number of consecutive color datas
        // [4*n] n*4 bytes representing the color data, in RGBA form
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
                        val c = colors[caret+i]!!
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
}