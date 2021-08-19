package spirite.consoleApp.fileValidation

import rbJvm.file.util.toBufferedRead
import rbJvm.file.util.toBufferedWrite
import rbJvm.file.util.toWrite
import spirite.core.file.SifConstants
import spirite.core.file.contracts.SifFile
import spirite.core.file.load.SifFileReader
import spirite.core.file.save.SifFileWriter
import java.io.File
import java.io.RandomAccessFile

// This suite does validation by reading into Sif Files and writing to Sif Files, recording any inability to read and
// recording any deviations between output files.  Never touches the ImageWorkspace and never turns raw data into images.
object PureSifFileValidation {
    var bufferFileLocation : File = File("E:\\Bucket\\sif")

    fun runSubFolderValidation(folder: File, outputFile: File) {
        if( outputFile.exists())
            outputFile.delete()
        outputFile.createNewFile()
        val sb = StringBuilder()

        fun runOnFile(file: File) {
            println(file.absolutePath)
            if( file.isDirectory ) {
                for (listFile in file.listFiles())
                    runOnFile(listFile)
            }
            else {
                if( file.extension.toLowerCase() in setOf("sif", "sif~")) {
                    validateFile(file, sb)
                    sb.appendln()
                }
            }
        }

        runOnFile(folder)


        outputFile.writeText(sb.toString())
    }

    fun validateFile(file: File, sb: StringBuilder) {
        sb.appendln(file.canonicalPath)
        val sifFile: SifFile

        // read
        try {
            sifFile = readSif(file)
        }
        catch ( e : Exception) {
            sb.appendln("Could not read in Sif File Because: ${e.message}")
            return
        }

        val origFile: File
        val destFile: File

        if( sifFile.version == SifConstants.latestVersion) {
            origFile = file
            destFile = File(bufferFileLocation.canonicalPath + "\\" + "fileValidationTest.sif")
            if( destFile.exists())
                destFile.delete()
            destFile.createNewFile()

            try { writeSif(destFile, sifFile)}
            catch (e: Exception) {
                sb.appendln("Could not Write File (lv) because: ${e.message}")
            }
        }
        else {
            origFile = File(bufferFileLocation.canonicalPath + "\\" + "fileValidationTest-1.sif")
            destFile = File(bufferFileLocation.canonicalPath + "\\" + "fileValidationTest-2.sif")

            if(origFile.exists())
                origFile.delete()
            origFile.createNewFile()
            if(destFile.exists())
                destFile.delete()
            destFile.createNewFile()

            val newSif: SifFile
            try {
                writeSif(origFile, sifFile)
                newSif = readSif(origFile)
                writeSif(destFile, newSif)
            }catch (e : Exception) {
                sb.appendln("Could not Read-Write File (ov) because: ${e.message}")
            }
        }

        binCompare(origFile, destFile, sb)

    }

    fun binCompare( file1: File, file2: File, sb: StringBuilder) {
        val len1 = file1.length()
        val len2 = file2.length()
        if (len1 != len2) {
            sb.appendln("File Lengths do not match: $len1, $len2")
        }

        val ra1 = RandomAccessFile(file1, "r")
        val ra2 = RandomAccessFile(file2, "r")
        try {
            val byteArray1 = ByteArray(10240)
            val byteArray2 = ByteArray(10240)
            while(ra1.filePointer < len1) {
                val read1 = ra1.read(byteArray1)
                val read2 = ra2.read(byteArray2)
                for (i in 0 until read1) {
                    if( byteArray1[i] != byteArray2[i]) {
                        val loc = (ra1.filePointer - read1 + i).toString(16)
                        sb.appendln("ne: $loc")
                    }
                }

                if( read1 != 10240 || read2 != 10240){
                    break
                }
            }

        } finally {
            ra1.close()
            ra2.close()
        }
    }

    fun writeSif(file: File, sifFile: SifFile) {
        val buffered = file.toBufferedWrite()
        try {
            val write = buffered.toWrite()
            SifFileWriter.write(write, sifFile)
        } finally { buffered.close() }
    }

    fun readSif(file: File) : SifFile{
        val read = file.toBufferedRead()
        try {
            return  SifFileReader.read(read)
        } finally{ read.close()}
    }
}