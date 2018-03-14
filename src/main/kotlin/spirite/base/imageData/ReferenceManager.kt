package spirite.base.imageData

interface IReferenceManager {
    var editingReference : Boolean

    fun zoomTransform( zoom: Float, centerX: Int, centerY: Int)
    fun rotateTransform( theta: Float,  centerX: Int, centerY: Int)
    fun shiftTransform( shiftX: Float, shiftY: Float)
}

class ReferenceManager : IReferenceManager {
    override fun shiftTransform(shiftX: Float, shiftY: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rotateTransform(theta: Float, centerX: Int, centerY: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var editingReference: Boolean = false

    override fun zoomTransform(zoom: Float, centerX: Int, centerY: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}