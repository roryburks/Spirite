package spirite.base.imageData.layers.sprite.tools

import spirite.base.imageData.layers.sprite.SpriteLayer

class SpriteLayerReconResults(
    val warnings: List<String>?,
    val errors: List<String>?,
    val map : Map<String,Int>)

interface ISpriteLayerReconService {
    fun generateProposal( sprites: List<SpriteLayer>) : SpriteLayerReconResults

    fun executeMap( map: Map<String, Int>)
}

object SpriteLayerReconService : ISpriteLayerReconService{
    override fun generateProposal(sprites: List<SpriteLayer>): SpriteLayerReconResults {
        val map = SpriteLayerNormalizer.getCanonicalMap(sprites, sprites.first())

        return SpriteLayerReconResults(null, null, map)
    }

    override fun executeMap(map: Map<String, Int>) {

    }
}