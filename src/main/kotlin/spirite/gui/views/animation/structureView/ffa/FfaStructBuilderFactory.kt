package spirite.gui.views.animation.structureView.ffa

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.animation.ffa.IFfaLayer


interface IFfaStructBuilderFactory {
    fun getFactoryForFfaLayer(layer: IFfaLayer) : IFFAStructViewBuilder
}

class FfaStructBuilderFactory( private  val _master: IMasterControl): IFfaStructBuilderFactory{
    override fun getFactoryForFfaLayer(layer: IFfaLayer) = when( layer) {
        is FFALayer -> FFAFlatLayerBuilder(_master)
        is FfaLayerCascading -> FfaCascadingLayerBuilder(_master)
        else -> TODO("Not Implemented")
    }
}