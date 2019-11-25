package rb.glow.exceptions


class GLResourcException(message: String?) : Exception(message)
class InvalidImageDimensionsExeption(message: String) : Exception(message)
open class GLEException(message: String) : Exception(message)