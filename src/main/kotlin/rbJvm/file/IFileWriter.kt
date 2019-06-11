package rbJvm.file

interface IFileWriter {

    fun writeFloatArray(data: FloatArray)
}

interface IFileReader {
    fun readFloatArray(len: Long) : FloatArray
}