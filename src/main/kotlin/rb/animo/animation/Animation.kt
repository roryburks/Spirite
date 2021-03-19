package rb.animo.animation

import rb.animo.DrawContract
import rb.glow.IGraphicsContext
import rb.vectrix.intersect.CollisionObject


interface IAnimation {
    val length: Double
    fun draw( gc: IGraphicsContext, met: Double) = draw(met, AnimationDrawSettings.Basic).forEach { it.drawRubrick(gc) }
    fun draw(met: Double, properties: AnimationDrawSettings, depth: Int = 0) : Collection<DrawContract>
    fun getHitbox(met: Double, type: Int) :  CollisionObject?
}

object NilAnimation : IAnimation {
    override val length: Double get() = 0.0

    override fun draw(met: Double, properties: AnimationDrawSettings, depth: Int) = emptySet<DrawContract>()
    override fun getHitbox(met: Double, type: Int) = null
}