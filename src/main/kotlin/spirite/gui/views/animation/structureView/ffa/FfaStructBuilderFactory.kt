package spirite.gui.views.animation.structureView.ffa

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.IFFALayer


interface IFfaStructBuilderFactory {
    fun GetFactoryForFfaLayer(layer: IFFALayer) : IFFAStructViewBuilder
}

class FfaStructBuilderFactory( private  val _master: IMasterControl): IFfaStructBuilderFactory{
    override fun GetFactoryForFfaLayer(layer: IFFALayer) = when( layer) {
        is FFALayer -> FFAFlatLayerBuilder(_master)
        else -> TODO("Not Implemented")
    }
}