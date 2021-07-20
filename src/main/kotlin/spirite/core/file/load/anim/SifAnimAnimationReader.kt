package spirite.core.file.load.anim

import rb.file.IReadStream
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifAnimAnimData

interface ISifAnimAnimationReader {
    fun read(read: IReadStream) : SifAnimAnimData
}

object SifAnimAnimationReaderFactory {
    fun getReader( typeId: Int, version: Int) : ISifAnimAnimationReader {
        return when(typeId){
            SifConstants.ANIM_FFA -> when {
                version < 8 -> LegacyFfaReader_X_to_7
                version <= 0x1_0000 -> LegacyFFAReader_8_TO_1_0000
                else -> FfaReader(version)
            }
            else -> {
                println("A")
                throw SifFileException("Unrecognized Animation Type: $typeId")
            }
        }
    }

}