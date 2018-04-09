package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext

interface ILiftedData {
    fun draw( gc: GraphicsContext)

    val width: Int
    val height: Int
}