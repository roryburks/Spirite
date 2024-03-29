package spirite.base.imageData.selection

import rb.glow.IGraphicsContext
import rb.vectrix.linear.ITransformF
import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.imageData.IImageWorkspace

interface ILiftedData {
    fun draw( gc: IGraphicsContext)
    fun bake( transform: ITransformF) : ILiftedData

    fun getImageDrawer(workspace: IImageWorkspace) : IImageDrawer

    val width: Int
    val height: Int
}