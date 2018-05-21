package spirite.base.pen.selectionBuilders

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection

abstract class SelectionBuilder( val workspace: IImageWorkspace) {
    abstract fun start( x: Int, y: Int)
    abstract fun update( x: Int, y: Int)
    abstract fun build() : Selection?
    abstract fun drawBuilding(gc: GraphicsContext)
}