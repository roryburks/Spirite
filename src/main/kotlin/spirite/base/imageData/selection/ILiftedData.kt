package spirite.base.imageData.selection

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.util.linear.Transform

interface ILiftedData {
    fun draw( gc: GraphicsContext)
    fun bake( transform: Transform) : ILiftedData

    fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer

    val width: Int
    val height: Int
}