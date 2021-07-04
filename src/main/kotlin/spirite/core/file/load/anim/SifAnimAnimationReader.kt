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
            SifConstants.ANIM_FFA -> FfaReader(version)
            else -> throw SifFileException("Unrecognized Animation Type: $typeId")
        }
    }

}