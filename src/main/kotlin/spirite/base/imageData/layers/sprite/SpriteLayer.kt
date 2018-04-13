package spirite.base.imageData.layers.sprite

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.util.StringUtil
import spirite.base.util.delegates.StackableUndoableDelegate
import spirite.base.util.groupExtensions.SinglyList
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Vec2
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType
import kotlin.math.roundToInt

/**
 *  A SpriteLayer is a collection of Dynamic
 */
class SpriteLayer(
        val workspace: IImageWorkspace,
        val mediumRepo: MMediumRepository,
        toImport: List<Pair<MediumHandle,SpritePartStructure>>? = null
) : Layer {

    val undoEngine = workspace.undoEngine
    val parts : List<SpritePart> get() = _parts
    private val _parts = mutableListOf<SpritePart>()
    var workingId = 0

    init {
        when( toImport) {
            null -> _parts.add(SpritePart(SpritePartStructure(0, "base"), mediumRepo.addMedium(DynamicMedium(workspace, mediumRepo = mediumRepo)), workingId++))
            else -> {
                toImport.forEach {_parts.add(SpritePart(it.second, it.first, workingId++))}
                _sort()
            }
        }
    }

    var activePart : SpritePart?
        get() = _parts.elementAtOrNull(activePartIndex)
        set(value) {
            when( value) {
                null -> activePartIndex = -1
                else -> _parts.indexOf(value)
            }
        }
    var activePartIndex: Int = -1 ; private set


    override val x: Int
        get() = 0
    override val y: Int
        get() = 0

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

    override val activeData: ArrangedMediumData
        get() {
        val part = activePart ?: parts.first()
        return ArrangedMediumData( part.handle, part.tPartToWhole)
    }

    override fun getDrawer(arranged: ArrangedMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val imageDependencies: List<MediumHandle> get() = parts.map { it.handle }

    override fun getDrawList(): List<TransformedHandle> {
        return parts
                .filter { it.isVisible }
                .map {TransformedHandle( it.handle, it.depth, it.tPartToWhole, it.alpha)}
    }


    override fun dupe(mediumRepo: MMediumRepository): Layer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    /** Returns the first highest-drawDepth part that is visible and has
     * non-transparent data at x, y (in Layer-space)*/
    fun grabPart( x: Int, y: Int, select: Boolean) {
        _parts.asReversed().forEach {
            // TODO
        }
    }

    fun addPart( partName : String) {
        val handle = mediumRepo.addMedium( DynamicMedium(workspace, mediumRepo = mediumRepo))
        val depth = when {
            _parts.isEmpty() -> 0
            activePartIndex == -1 -> _parts.last().depth + 1
            else -> activePart!!.depth
        }
        _addPart( SpritePartStructure( depth, partName), handle, if( activePartIndex == -1) _parts.size else activePart!!.depth + 1)
    }
    fun removePart( toRemove: SpritePart) {
        if( !_parts.contains(toRemove)) return

        undoEngine.performAndStore( object: NullAction() {
            override val description: String get() = "Remove Sprite Part"

            override fun performAction() {
                if( activePart == toRemove)
                    activePartIndex = -1
                _parts.remove(toRemove)
                triggerChange()
            }

            override fun undoAction() {
                // Note: Since _parts are automatically sorted by drawDepth, no need to remember its old drawDepth
                _parts.add(toRemove)
                _sort()
                triggerChange()
            }

            override fun getDependencies() = SinglyList(toRemove.handle)
        })

    }
    fun movePart( fromIndex: Int, toIndex: Int) {
        undoEngine.doAsAggregateAction({
            val toMove = _parts.get(fromIndex)
            removePart(_parts.get(fromIndex))
            _addPart(toMove.structure, toMove.handle, toIndex)  // TODO: I don't really like this as it doesn't preserve Part references
        }, "Moved Sprite Part")
    }

    private fun _addPart( structure: SpritePartStructure, handle: MediumHandle, index: Int) {
        if( _parts.size >= 255) {
            // Primarily for save/load simplicity
            MDebug.handleWarning(WarningType.UNSUPPORTED, "Only 255 parts per rig currently supported.")
            return
        }

        undoEngine.doAsAggregateAction({
            // Note: Because of how doAsAggregateAction prevents action until it's completed,
            //	the logic in this method is counter-intuitively valid, as all of the checks
            //	are done before the new part gets added, even though it's done using
            //	a performAndStore that happens before them.  Probably bad design.
            val name = StringUtil.getNonDuplicateName( _parts.map { it.partName }, structure.partName)
            val newStructure = structure.copy(partName =  name)

            val toAdd = SpritePart(newStructure, handle, workingId++)

            undoEngine.performAndStore( object: NullAction() {
                override val description: String get() = ""
                override fun performAction() {
                    _parts.add( index, toAdd)
                    _sort()
                    triggerChange()
                }

                override fun undoAction() {
                    _parts.remove(toAdd)
                    triggerChange()
                }

                override fun getDependencies() = SinglyList(toAdd.handle)
            })

        }, "Added New Part")
    }
    private fun _sort() {
        _parts.sortWith(compareBy({it.depth}, {it.id}))
    }


    inner class SpritePart(
            internal var structure: SpritePartStructure,
            val handle: MediumHandle,
            internal val id: Int)
    {

        // region Parotting SpritePartStructure scroll with Undoable Wrapper
        var depth : Int by StackableUndoableDelegate(structure.depth, undoEngine, "Changed Sprite Part depth")
        var visible : Boolean by StackableUndoableDelegate(structure.visible, undoEngine, "Changed Sprite Part visible")
        var partName : String by StackableUndoableDelegate(structure.partName, undoEngine, "Changed Sprite Part Name")
        var alpha : Float by StackableUndoableDelegate(structure.alpha, undoEngine, "Changed Sprite Part alpha")
        var transX : Float by StackableUndoableDelegate(structure.transX, undoEngine, "Changed Sprite Part transX")
        var transY : Float by StackableUndoableDelegate(structure.transY, undoEngine, "Changed Sprite Part transY")
        var scaleX : Float by StackableUndoableDelegate(structure.scaleX, undoEngine, "Changed Sprite Part scaleX")
        var scaleY : Float by StackableUndoableDelegate(structure.scaleY, undoEngine, "Changed Sprite Part scaleY")
        var rot : Float by StackableUndoableDelegate(structure.rot, undoEngine, "Changed Sprite Part rot")
        var centerX : Int by StackableUndoableDelegate(structure.centerX ?: 0, undoEngine, "Changed Sprite Part centerX")
        var centerY : Int by StackableUndoableDelegate(structure.centerY ?: 0, undoEngine, "Changed Sprite Part centerY")


        private fun replaceStructure( newStructure: SpritePartStructure) {
            if( structure != newStructure)
                undoEngine.performAndStore(SpriteStructureAction(newStructure))
        }

        inner class SpriteStructureAction(
                var newStructure: SpritePartStructure
        ) : NullAction(), StackableAction {
            val oldStructure: SpritePartStructure = structure
            override val description: String get() = "Change Part Structure"
            override fun performAction() {structure = newStructure ; _sort()}
            override fun undoAction() {structure = oldStructure ; _sort()}

            override fun canStack(other: UndoableAction) = (other as? SpriteStructureAction)?._context == this._context
            override fun stackNewAction(other: UndoableAction) {newStructure = (other as SpriteStructureAction).newStructure}
            private val _context: SpriteLayer get() = this@SpriteLayer
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
        val centerY: Int? = null)
