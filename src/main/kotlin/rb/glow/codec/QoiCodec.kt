package rb.glow.codec

import rb.extendo.kiebabo.BitPacking
import rb.file.*
import rb.vectrix.mathUtil.b
import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.ui

// Technically wrong thins with official QOI format:
// 1: I'm using rgb = 0, a = 0 for start rather than rgb = 0, a = 255
// 2: I'm not using QOI_OP_RGB when encoding (just using RGBA)

class QoiCodec : IImageCodec{
    override fun encode(data: CodecImageData): ByteArray {
        // Meh for now
        //if( data.premultipliedAlpha)
        //    TODO("A de-multiplication action is needed; currently not implemented")

        val channels = when( data.format) {
            CodecImageFormat.ARGB -> 4
            CodecImageFormat.RGB -> {3 ; throw NotImplementedError("Not doing RGB yet")}
            else -> throw NotImplementedError("Unexpected CodecFormat: ${data.format}")
        }

        val underlying = ByteListWriteStream()
        val raw = BufferedWriteStream(underlying)
        val write = BigEndianWriteStream(raw)

        // Debugging Counters
        var code_cacheHits = 0
        var code_runs = 0
        var code_deltas = 0
        var code_lums = 0
        var code_rgb = 0
        var j = 0

        // Header
        write.write(Header)
        write.writeInt(data.width)
        write.writeInt(data.height)
        write.writeByte(channels)
        write.writeByte(0) // ColorSpace; not sure I'm using this correct, but w/e

        // In-mem data
        val cache = IntArray(64) {PixelBase}
        var pixelBuff = PixelBase
        var runlengthBuff = 0

        fun writeRunlength(){
            val runlengthCode = 0xc0 or (runlengthBuff -1)
            write.writeByte(runlengthCode)
            ++code_runs
            runlengthBuff = 0
        }

        fun runPixel(r : Byte, g: Byte, b: Byte, a: Byte){
            val color = BitPacking.packInt(r,g,b,a)
            val hash = hash(r,g,b,a)
            val previousPixel = pixelBuff
            pixelBuff = color
            if( previousPixel == color)
            {
                ++runlengthBuff
                if( runlengthBuff == 62)
                    writeRunlength()
                return
            }
            ++j
            if( runlengthBuff != 0)
                writeRunlength()
            if( cache[hash] == color)
            {
                // QOI_OP_INDEX chunk [ 0 0 : hash-index] = hash-index
                write.writeByte(hash)
                ++code_cacheHits
                return
            }
            else
            {
                cache[hash] = color
                val pr = (previousPixel shr 0).toByte()
                val pg = (previousPixel shr 8).toByte()
                val pb = (previousPixel shr 16).toByte()
                val pa = (previousPixel shr 24).toByte()

                if( pa == a)
                {
                    val dr = r - pr
                    val dg = g - pg
                    val db = b - pb
                    if( dr >= -2 && dr <= 1 && dg >= -2 && dg <= 1 && db >= -2 && db <= 1)
                    {
                        // QOI_OP_DIFF
                        val code = 0x40 and
                                ( (dr + 2) shl 4) and
                                ( (dg + 2) shl 2) and
                                (db + 2)
                        write.writeByte(code)
                        ++code_deltas
                        return
                    }

                    if( dg >= -32 && dg <= 31 )
                    {
                        val lumR = dr - dg
                        val lumB = db - dg

                        if( lumR >= -8 && lumR <= 7 && lumB >= -8 && lumB <= 7) {
                            // QOI_OP_LUMA
                            val codeGreen = 0x80 and (dg + 32)
                            val codeLum = ((lumR + 8) shl 4) and ((lumB + 8))
                            write.writeByte(codeGreen)
                            write.writeByte(codeLum)
                            ++code_lums
                            return
                        }
                    }
                }

                // QOI_OP_RGBA
                write.writeByte(0xff)
                write.writeByte(r.i)
                write.writeByte(g.i)
                write.writeByte(b.i)
                write.writeByte(a.i)
                ++code_rgb
            }


        }

        var i = 0
        for (x in 0 until data.width)
            for (y in 0 until data.height) {
                val base = (y*data.width+x)*4
                ++i
                runPixel( data.raw[base + 1], data.raw[base + 2], data.raw[base + 3], data.raw[base + 0] )
            }

        if( runlengthBuff != 0)
            writeRunlength()

        write.write(Footer)

        raw.finish()
        return underlying.list.toByteArray()
    }

    override fun decode(data: ByteArray): CodecImageData {
        val binRead = ByteArrayReadStream(data)
        val read = BufferedReadStream(binRead)

        val magicNum = read.readByteArray(4)
        if( magicNum[0] != Header[0] || magicNum[1] != Header[1] || magicNum[2] != Header[2] || magicNum[3] != Header[3])
            throw Exception("Header does not match QOI header")

        val width = read.readInt()
        val height = read.readInt()
        val channels = read.readByte()
        val colorspace = read.readByte() // unused

        if( channels.i != 4)
            throw Exception("Cannot support QOI Channel type: $channels")

        val cache = IntArray(64) {PixelBase}
        var previousColor = PixelBase

        val output = ByteArray(width*height*4)
        var caret = 0

        fun writeColor(r: Byte, g: Byte, b: Byte, a: Byte) {
            output[caret*4 + 0] = a
            output[caret*4 + 1] = r
            output[caret*4 + 2] = g
            output[caret*4 + 3] = b
            ++caret

            val thisColor = BitPacking.packInt(r,g,b,a)
            previousColor = thisColor
            cache[hash(r,g,b,a)] = thisColor
        }

        lo@ while( caret < width*height)
        {
            val code = read.readByte().ui
            if( code == 0xFF || code == 0xFE)
            {
                val r = read.readByte()
                val g = read.readByte()
                val b = read.readByte()
                val a = if( code == 0xFF) read.readByte()
                    else BitPacking.unpackInt(previousColor)[3]
                writeColor(r, g, b, a)
            }
            else when( code and 0xc0){
                0 -> { //cache
                    val ind = code and 0x3f
                    val rgba = BitPacking.unpackInt(cache[ind])
                    writeColor(rgba[0], rgba[1], rgba[2], rgba[3])
                }
                0x40 -> { // dif
                    val dr = ((code and 0x30) shr 4) - 2
                    val dg = ((code and 0xC) shr 2) - 2
                    val db = (code and 0x3) - 2
                    val rgba = BitPacking.unpackInt(previousColor)
                    writeColor(
                        ((rgba[0]+dr) % 256).b,
                        ((rgba[1]+dg) % 256).b,
                        ((rgba[2]+db) % 256).b,
                        rgba[3])
                }
                0x80 -> { // LUMA
                    val code2 = read.readByte().ui
                    val dg = (code and 0x3f) - 32
                    val lumR = (code2 and 0xf0) - 8
                    val lumB = (code2 and 0x0f) - 8
                    val rgba = BitPacking.unpackInt(previousColor)

                    writeColor(
                        ((rgba[0] + dg + lumR) % 256).b,
                        ((rgba[1] + dg) % 256).b,
                        ((rgba[2] + dg + lumB) % 256).b,
                        rgba[3] )
                }
                0xc0 -> { // runlen
                    val len = (code and 0x3f) +1
                    val rgba = BitPacking.unpackInt(previousColor)
                    try {
                        repeat(len) { writeColor(rgba[0], rgba[1], rgba[2], rgba[3]) }
                    }catch (e: Throwable)
                    {
                        println("BAD")
                        break@lo
                    }
                }
                else -> throw Exception("Failure when reading: I'm bad at math")
            }
        }

        return  CodecImageData(
            width,
            height,
            output,
            CodecImageFormat.ARGB,
            false )
    }


    companion object {
        val Header = byteArrayOf( 0x71, 0x6f, 0x69, 0x66)
        val Footer = byteArrayOf( 0,0,0,0,0,0,0,0x01)
        val PixelBase = 0

        fun hash(r : Byte, g: Byte, b : Byte, a: Byte) = (r.ui*3 + g.ui*5 + b.ui*7 + a.ui*11) % 64
    }
}