package spirite.base.imageData.drawer

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.imageData.drawer.IImageDrawer.ITransformModule

class MultiMediumDrawer(
        val subDrawers: List<IImageDrawer>,
        val cx: Float = 320f,
        val cy: Float = 240f) : IImageDrawer,
        ITransformModule {
    override fun transform(trans: ITransformF, centered: Boolean) {

        val effectiveTrans =
                if( centered) ImmutableTransformF.Translation(cx,cy) * trans * ImmutableTransformF.Translation(-cx,-cy)
                else trans

        subDrawers
                .filterIsInstance<ITransformModule>()
                .forEach { it.transform(effectiveTrans, false) }
    }

    override fun startManipulatingTransform() = null

    override fun stepManipulatingTransform() {}

    override fun endManipulatingTransform() {}
}