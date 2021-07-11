package spirite.core.file

class SifFileException(message: String) : Exception(message) {
    var version: Int = -1
}