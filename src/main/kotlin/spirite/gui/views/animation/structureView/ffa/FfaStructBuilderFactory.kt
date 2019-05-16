package spirite.gui.views.animation.structureView.ffa

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.animation.ffa.IFfaLayer


interface IFfaStructBuilderFactory {
    fun getFactoryForFfaLayer(layer: IFfaLayer) : IFfaStructViewBuilder
}

class FfaStructBuilderFactory(
        private  val _master: IMasterControl,
        private  val _rebuildTrigger: ()->Unit)
    : IFfaStructBuilderFactory
{
    override fun getFactoryForFfaLayer(layer: IFfaLayer) = when( layer) {
        is FFALayer -> FFAFlatLayerBuilder(_master)
        is FfaLayerCascading -> FfaCascadingLayerBuilder(_master, _rebuildTrigger)
        else -> TODO("Not Implemented")
    }
}