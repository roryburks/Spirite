package spirite.core.file.save

import rb.file.IWriteStream
import spirite.core.file.SifConstants
import spirite.core.file.contracts.SifFile

object SifFileWriter
{
    fun write(out: IWriteStream, file: SifFile) {
        // Header
        out.write(SifConstants.header)

        //

    }

}