package spirite.base.image_data.animations.rig

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle
import spirite.base.image_data.Animation
import spirite.base.image_data.GroupTree.LayerNode
import spirite.base.image_data.ImageWorkspace
import spirite.base.util.linear.MatTrans

class RigAnimation( name: String, context: ImageWorkspace) : Animation( name, context) {
    private val rigLayers = ArrayList<RigAnimLayer>()
    val spriteLayers get() = rigLayers.map { it.sprite }

    override val StartFrame: Float = 0f
    override val EndFrame: Float = 10f
    override val isFixedFrame: Boolean = false

    override fun getDrawList(t: Float): List<TransformedHandle> {
        val list = ArrayList<TransformedHandle>()

        for( layer in rigLayers) {
            for( part in layer.map) {
                val pstruct = part.key.structure    // NOTE: Copy-property
                val keyframes = part.value

                val key = keyframes.getFrameAtT(t)

                val trans = MatTrans()
                trans.preRotate(key.rot)
                trans.preScale(key.sx,key.sy)
                trans.preTranslate(key.tx, key.ty)

                val th = TransformedHandle()
                th.alpha = pstruct.alpha
                th.depth = pstruct.depth
                th.handle = pstruct.handle
                th.trans = trans
                list.add(th)
            }
        }
        return list
    }

    fun addLayer( node: LayerNode) : RigAnimLayer {
        val ral = RigAnimLayer(node)
        rigLayers.add( ral)
        return ral
    }

    // TODO
    fun purge() {
        rigLayers.removeIf { !context.nodeInWorkspace(it.layer)}
    }
}