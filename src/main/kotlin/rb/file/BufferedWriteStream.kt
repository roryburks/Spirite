package rb.file

import rb.vectrix.VectrixMathLayer

class BufferedWriteStream(
    val underlying: IRawWriteStream,
    val buffSize: Int = 2048) : IRawWriteStream
{
    override var pointer: Long  = 0; private set

    private var buff: ByteArray? = null
    private var buffMet = 0

    override fun goto(pointer: Long) {
        writeInBuff()
        underlying.goto(pointer)
        this.pointer = pointer
        buff = null
        buffMet = 0
    }

    override fun write(byteArray: ByteArray) {
        writeSub(byteArray, 0)
        pointer += byteArray.size
    }
    private tailrec fun writeSub(byteArray: ByteArray, startPos: Int){
        val oBuff = buff
        val numBytesToWrite = byteArray.size - startPos

        if( oBuff == null) {
            if( numBytesToWrite >= buffSize){
                val toWrite = ByteArray(numBytesToWrite)
                VectrixMathLayer.arraycopy(byteArray,startPos,toWrite,0,numBytesToWrite)
                underlying.write(toWrite)
            }
            else {
                val nBuff = ByteArray(buffSize)
                VectrixMathLayer.arraycopy(byteArray,startPos,nBuff,0,numBytesToWrite)
                buff = nBuff
                buffMet = numBytesToWrite
            }
        }
        else {
            if( numBytesToWrite + buffMet >= buffSize) {
                val numBytesWriting = buffSize - buffMet
                val fullWrite = numBytesToWrite + buffMet == buffSize
                VectrixMathLayer.arraycopy(byteArray,startPos,oBuff,buffMet,numBytesWriting)
                underlying.write(oBuff)
                buff = null
                buffMet = 0
                if( !fullWrite)
                    writeSub(byteArray, numBytesWriting)
            }
            else {
                VectrixMathLayer.arraycopy(byteArray,startPos,oBuff,buffMet,numBytesToWrite)
                buffMet += numBytesToWrite
            }
        }
    }

    private fun writeInBuff() {
        val oBuff = buff
        if( oBuff != null && buffMet != 0) {
            val toWrite = ByteArray(buffMet)
            VectrixMathLayer.arraycopy(oBuff,0, toWrite, 0, buffMet)
            underlying.write(toWrite)
        }
    }

    override fun finish() {
        writeInBuff()
        underlying.finish()
    }

    override fun close() {
        finish()
        underlying.close()
    }
}