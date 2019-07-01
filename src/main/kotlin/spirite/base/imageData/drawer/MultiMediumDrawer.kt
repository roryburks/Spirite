package spirite.base.imageData.drawer

import rb.vectrix.linear.ITransformF
import spirite.base.imageData.drawer.IImageDrawer.ITransformModule
import spirite.base.util.linear.Rect

class MultiMediumDrawer(val subDrawers: List<IImageDrawer>) : IImageDrawer,
        ITransformModule {
    override fun transform(trans: ITransformF, centered: Boolean) {
        subDrawers
                .filterIsInstance<ITransformModule>()
                .forEach { it.transform(trans, centered) }
    }

    override fun startManipulatingTransform() = null

    override fun stepManipulatingTransform() {}

    override fun endManipulatingTransform() {}
}