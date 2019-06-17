package spirite.base.imageData.selection

import rb.vectrix.linear.ITransformF
import rb.glow.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer

interface ILiftedData {
    fun draw( gc: GraphicsContext)
    fun bake( transform: ITransformF) : ILiftedData

    fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer

    val width: Int
    val height: Int
}