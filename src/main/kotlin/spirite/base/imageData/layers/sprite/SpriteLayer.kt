package spirite.base.imageData.layers.sprite

import spirite.base.graphics.DynamicImage
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.BuildingMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoEngine
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Vec2
import kotlin.math.roundToInt

/**
 *  A SpriteLayer is a collection of Dynamic
 */
class SpriteLayer(
        val workspace: IImageWorkspace,
        val mediumRepo: MMediumRepository
) : Layer() {
    val parts : List<SpritePart> get() = _parts
    private val _parts = mutableListOf<SpritePart>()

    private var activePart: SpritePart? = null



    override val width: Int get() {
        val xs = mutableListOf<Float>()
        parts.forEach {
            val tPartToWhole = it.tPartToWhole
            xs.add( tPartToWhole.apply(Vec2(0f,0f)).x)
            xs.add( tPartToWhole.apply(Vec2(0f,it.handle.height+0f)).x)
            xs.add( tPartToWhole.apply(Vec2(it.handle.width+0f,0f)).x)
            xs.add( tPartToWhole.apply(Vec2(it.handle.width+0f,it.handle.height+0f)).x)
        }
        return xs.map { Math.ceil(it.toDouble()).roundToInt() }.max() ?: 0 -
                (xs.map {Math.floor( it.toDouble()).roundToInt()}.min() ?: 0)
    }
    override val height: Int get() {
        val ys = mutableListOf<Float>()
        parts.forEach {
            val tPartToWhole = it.tPartToWhole
            ys.add(tPartToWhole.apply(Vec2(0f, 0f)).y)
            ys.add(tPartToWhole.apply(Vec2(0f, it.handle.height + 0f)).y)
            ys.add(tPartToWhole.apply(Vec2(it.handle.width + 0f, 0f)).y)
            ys.add(tPartToWhole.apply(Vec2(it.handle.width + 0f, it.handle.height + 0f)).y)
        }
        return ys.map { Math.ceil(it.toDouble()).roundToInt() }.max() ?: 0-
        (ys.map { Math.floor(it.toDouble()).roundToInt() }.min() ?: 0)
    }

    override val activeData: BuildingMediumData get() {
        var part = activePart ?: parts.first()
        return BuildingMediumData( part.handle, part.tPartToWhole)
    }

    override fun getDrawer(building: BuildingMediumData, medium: IMedium): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val imageDependencies: List<MediumHandle> get() = parts.map { it.handle }

    override fun getDrawList(): List<TransformedHandle> {
        return parts
                .filter { it.isVisible }
                .map {TransformedHandle( it.handle, it.alpha, it.tPartToWhole, depth = it.depth)}
    }


    /** Returns the first highest-depth part that is visible and has
     * non-transparent data at x, y (in Layer-space)*/
    fun grabPart( x: Int, y: Int, select: Boolean) {
        _parts.asReversed().forEach {
            // TODO
        }
    }

    fun addPart( partName : String) {
        val handle = mediumRepo.addMedium( DynamicMedium(workspace))
    }
    fun removePart( toRemove: SpritePart) {

    }
    fun movePart( fromIndex: Int, toIndex: Int) {

    }
}

data class SpritePartStructure(
        val depth: Int,
        val partName: String,
        val visible: Boolean = true,
        val alpha: Float = 1f,
        val transX: Float = 0f,
        val transY: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rot: Float = 0f,
        val centerX: Int? = null,
        val centerY: Int? = null
)

class SpritePart(
        structure: SpritePartStructure,
        val handle: MediumHandle,
        val undoEngine: UndoEngine?)
{
    private var structure = structure

    // region Parotting SpritePartStructure value with Undoable Wrapper
    var depth : Int
        get() = structure.depth
        set(value) { replaceStructure(structure.copy(depth = value))}
    var partName : String
        get() = structure.partName
        set(value) { replaceStructure(structure.copy(partName = value))}
    var visible : Boolean
        get() = structure.visible
        set(value) { replaceStructure(structure.copy(visible = value))}
    var alpha : Float
        get() = structure.alpha
        set(value) { replaceStructure(structure.copy(alpha = value))}
    var transX : Float
        get() = structure.transX
        set(value) { replaceStructure(structure.copy(transX = value))}
    var transY : Float
        get() = structure.transY
        set(value) { replaceStructure(structure.copy(transY = value))}
    var scaleX : Float
        get() = structure.scaleX
        set(value) { replaceStructure(structure.copy(scaleX = value))}
    var scaleY : Float
        get() = structure.scaleY
        set(value) { replaceStructure(structure.copy(transY = scaleY))}
    var rot : Float
        get() = structure.rot
        set(value) { replaceStructure(structure.copy(transY = rot))}
    var centerX : Int?
        get() = structure.centerX
        set(value) { replaceStructure(structure.copy(centerX = value))}
    var centerY : Int?
        get() = structure.centerY
        set(value) { replaceStructure(structure.copy(centerY = value))}

    private fun replaceStructure( newStructure: SpritePartStructure) {
        when( undoEngine) {
            null -> structure = newStructure
            else -> undoEngine.performAndStore(SpriteStructureAction(newStructure))
        }
    }

    inner class SpriteStructureAction(
            var newStructure: SpritePartStructure
    ) : NullAction() {
        val oldStructure: SpritePartStructure = structure
        override val description: String get() = "Change Part Structure"
        override fun performAction() {structure = newStructure}
        override fun undoAction() {structure = oldStructure}

    }
    // endregion

    val isVisible: Boolean get() = structure.visible && structure.alpha != 0f
    val tPartToWhole : MutableTransform get() {
        val cx = structure.centerX?.toFloat() ?: handle.width/2f
        val cy = structure.centerY?.toFloat() ?: handle.height/2f
        val ret = MutableTransform.TranslationMatrix(-cx, -cy)
        ret.preScale(structure.scaleX, structure.scaleY)
        ret.preRotate( structure.rot)
        ret.preTranslate( structure.transX + handle.width/2f, structure.transY + handle.height/2f)
        return  ret
    }

    val tWholeToPart : MutableTransform get() = tPartToWhole.invertM()

}