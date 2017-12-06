package spirite.base.image_data.animations.rig

import spirite.base.image_data.GroupTree.LayerNode
import spirite.base.image_data.layers.SpriteLayer
import spirite.base.image_data.layers.SpriteLayer.Part

class RigAnimLayer(
        val layer:LayerNode
)
{
    val sprite = (layer.layer as SpriteLayer)
    internal val map = HashMap<Part, RigKeyframeSet>()
    val partMap : Map<Part,RigKeyframeSet> get() =  map.toMap()
}