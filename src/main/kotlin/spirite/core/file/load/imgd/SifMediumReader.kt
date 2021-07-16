package spirite.core.file.load.imgd

import rb.file.IReadStream
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifImgdMed_Dynamic
import spirite.core.file.contracts.SifImgdMed_Plain
import spirite.core.file.contracts.SifImgdMed_Prismatic
import spirite.core.file.contracts.SifImgdMediumData

interface ISifMediumReader {
    fun read(read: IReadStream) :SifImgdMediumData
}

object SifMediumReaderFactory {
    fun getMediumReader(version: Int, typeId: Int) : ISifMediumReader = when(typeId){
        SifConstants.MEDIUM_PLAIN -> SifMedReader_Flat//.also { println("Plain") }
        SifConstants.MEDIUM_DYNAMIC -> SifMedReader_Dynamic//.also { println("Dynamic") }
        SifConstants.MEDIUM_PRISMATIC -> SifMedReader_Prismatic//.also { println("Prismatic") }
        SifConstants.MEDIUM_MAGLEV -> when {
            version < 0x1_0000 -> SifMaglevMediumReader_Legacy_Pre_1_0000(version)//.also { println("LegLeg") }
            version <= 0x1_0006 -> SifMaglevMediumReader_Legacy_1_0000_To_1_0006(version)//.also { println("Leg") }
            else -> SifMaglevMediumReader(version)//.also { println("new") }
        }
        else -> throw SifFileException("Unrecognized Medium Type Id: $typeId.  Trying to load a newer SIF version in an older program version or corrupt file.")
    }

}

object SifMedReader_Flat : ISifMediumReader {
    override fun read(read: IReadStream): SifImgdMediumData {
        val imgSize = read.readInt()
        val raw = read.readByteArray(imgSize)
        return SifImgdMed_Plain(raw)
    }
}

object SifMedReader_Dynamic : ISifMediumReader {
    override fun read(read: IReadStream): SifImgdMediumData {
        val ox = read.readShort()
        val oy = read.readShort()
        val imgSize = read.readInt()
        val raw = if( imgSize == 0) null else read.readByteArray(imgSize)

        return SifImgdMed_Dynamic(ox, oy, raw)
    }
}

object SifMedReader_Prismatic : ISifMediumReader {
    override fun read(read: IReadStream): SifImgdMediumData {
        val numLayers = read.readUnsignedShort()
        repeat(numLayers){
            read.readInt()
            read.readShort()
            read.readShort()
            val imgSize = read.readInt()
            read.filePointer = read.filePointer + imgSize
        }

        return SifImgdMed_Prismatic
    }
}