package spirite.specialRendering.fill

interface IFillArrayAlgorithm {
    fun fill( data: IntArray, w: Int, h: Int, x: Int, y: Int, color: Int) : IntArray?
}

