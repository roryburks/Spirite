package rb.animo.animation

import rb.animo.DrawContract
import rb.extendo.dataStructures.SinglyCollection
import rb.glow.IGraphicsContext
import rb.glow.gl.GLImage

class FlatAnimation( val img: GLImage)
    : IAnimation
{
    override val length: Double get() = 0.0
    override fun draw(gc: IGraphicsContext, met: Double) {
        gc.renderImage(img, 0.0, 100.0)
    }

    override fun draw(met: Double, properties: AnimationDrawSettings, depth: Int) = SinglyCollection(DrawContract(depth, properties){draw(it,0.0)})

    override fun getHitbox(met: Double, type: Int) = null
}