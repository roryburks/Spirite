package spirite.base.imageData.layers.sprite

import spirite.base.brains.Bindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.util.StringUtil
import spirite.base.util.ceil
import spirite.base.util.floor
import spirite.base.util.groupExtensions.SinglyList
import spirite.base.util.groupExtensions.mapAggregated
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Vec2
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType

/**
 *  A SpriteLayer is a collection of Dynamic Mediums with various offsets, transforms, and
 */
class SpriteLayer(
        val workspace: IImageWorkspace,
        val mediumRepo: MMediumRepository,
        toImport: List<Pair<MediumHandle,SpritePartStructure>>? = null
) : Layer {

    val undoEngine = workspace.undoEngine
    val parts : List<SpritePart> get() = _parts
    private val _parts = mutableListOf<SpritePart>()
    private var workingId = 0

    init {
        when( toImport) {
            null -> _parts.add(SpritePart(SpritePartStructure(0, "base"), mediumRepo.addMedium(DynamicMedium(workspace, mediumRepo = mediumRepo)), workingId++))
            else -> {
                toImport.forEach {_parts.add(SpritePart(it.second, it.first, workingId++))}
                _sort()
            }
        }
    }

    var activePartBind =  Bindable<SpritePart?>(null) { new, _ ->
        cAlphaBind.field = new?.alpha ?: 1f
        cDepthBind.field = new?.depth ?: 0
        cVisibleBind.field = new?.visible ?: true
        cPartNameBind.field = new?.partName ?: ""
        cTransXBind.field = new?.transX ?: 0f
        cTransYBind.field = new?.transY ?: 0f
        cScaleXBind.field = new?.scaleX ?: 1f
        cScaleYBind.field = new?.scaleY ?: 1f
        cRotBind.field = new?.rot ?: 0f
    }
    var activePart : SpritePart? by activePartBind

    private var _layerChangeObserver = Observable<()->Any?>()
    val layerChangeObserver : IObservable<()->Any?> get() = _layerChangeObserver
    override fun triggerChange() {
        _layerChangeObserver.trigger { it() }
        workspace.imageObservatory.triggerRefresh(
                ImageChangeEvent(
                        emptyList(),
                        workspace.groupTree.root.getAllNodesSuchThat({(it as? LayerNode)?.layer == this}),
                        workspace)
        )
        super.triggerChange()
    }



    // region ILayer methods

//    private val _keyPointsDerived = DerivedLazy{ etc }
    private val _keyPoints get() = parts.mapAggregated {
            val tPartToWhole = it.tPartToWhole
            listOf(Vec2(0f,0f), Vec2(0f, it.handle.height+0f), Vec2( it.handle.width+0f,0f), Vec2(it.handle.width+0f, it.handle.height+0f))
                    .map { tPartToWhole.apply(it)}}

    override val x: Int get() = _keyPoints.map { it.x.floor }.min() ?: 0
    override val y: Int get() = _keyPoints.map { it.y.floor }.min() ?: 0

    override val width: Int get() {
        val xs = _keyPoints.map { it.x.ceil }
        return (xs.max() ?: 0) - (xs.min() ?: 0)
    }
    override val height: Int get() {
        val ys = _keyPoints.map { it.y.ceil }
        return (ys.max() ?: 0) - (ys.min() ?: 0)
    }

    override val activeData: ArrangedMediumData
        get() {
        val part = activePart ?: parts.first()
        return ArrangedMediumData( part.handle, part.tPartToWhole)
    }

    override fun getDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)
    override val imageDependencies: List<MediumHandle> get() = parts.map { it.handle }
    override fun getDrawList() = parts
                .filter { it.isVisible }
                .map {TransformedHandle( it.handle, it.depth, it.tPartToWhole, it.alpha)}

    override fun dupe(mediumRepo: MMediumRepository) =
            SpriteLayer( workspace, mediumRepo, parts.map { Pair(mediumRepo.addMedium(it.handle.medium.dupe()), SpritePartStructure(it)) })
    // endregion


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
            activePart == null -> _parts.last().depth + 1
            else -> activePart!!.depth
        }

        undoEngine.doAsAggregateAction("Add Sprite Part") {
            _addPart( SpritePartStructure( depth, partName), handle)
        }
    }
    fun removePart( toRemove: SpritePart) {
        if( !_parts.contains(toRemove)) return

        undoEngine.performAndStore( object: NullAction() {
            override val description: String get() = "Remove Sprite Part"

            override fun performAction() {
                if( activePart == toRemove)
                    activePart = null
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
        undoEngine.doAsAggregateAction("Moved Sprite Part"){
            val toMove = _parts.get(fromIndex)
            removePart(_parts.get(fromIndex))
            _addPart(toMove.structure, toMove.handle)  // TODO: I don't really like this as it doesn't preserve Part references
        }
    }

    private fun _addPart( structure: SpritePartStructure, handle: MediumHandle) {
        if( _parts.size >= 255) {
            // Primarily for save/load simplicity
            MDebug.handleWarning(WarningType.UNSUPPORTED, "Only 255 parts per rig currently supported.")
            return
        }

        val name = StringUtil.getNonDuplicateName( _parts.map { it.partName }, structure.partName)
        val newStructure = structure.copy(partName =  name)
        val toAdd = SpritePart(newStructure, handle, workingId++)


        undoEngine.performAndStore( object: NullAction() {
            override val description: String get() = ""
            override fun performAction() {
                _parts.add(toAdd)
                _sort()
                triggerChange()
            }

            override fun undoAction() {
                _parts.remove(toAdd)
                triggerChange()
            }

            override fun getDependencies() = SinglyList(toAdd.handle)
        })
    }
    private fun _sort() {
        _parts.sortWith(compareBy({it.depth}, {it.id}))
    }

    val cDepthBind = Bindable(0) {new, _ ->  activePart?.depth = new}
    val cVisibleBind = Bindable(true) {new, _ ->  activePart?.visible = new}
    val cPartNameBind = Bindable("") {new, _ ->  activePart?.partName = new}
    val cAlphaBind = Bindable(1f) {new, _ ->  activePart?.alpha = new}
    val cTransXBind = Bindable(0f) {new, _ ->  activePart?.transX = new}
    val cTransYBind = Bindable(0f) {new, _ ->  activePart?.transY = new}
    val cScaleXBind = Bindable(1f) {new, _ ->  activePart?.scaleX = new}
    val cScaleYBind = Bindable(1f) {new, _ ->  activePart?.scaleY = new}
    val cRotBind = Bindable(0f) {new, _ ->  activePart?.rot = new}



    inner class SpritePart(
            _structure: SpritePartStructure,
            val handle: MediumHandle,
            internal val id: Int)
    {
        var structure = _structure ; private set

        // region Shadowing SpritePartStructure scroll with Undoable Wrapper
        var depth get() = structure.depth ; set(value) { if( value != structure.depth) replaceStructure(structure.copy(depth = value), 0)}

        var visible get() = structure.visible ; set(value) { if( value != structure.visible) replaceStructure(structure.copy(visible = value), 1)}
        var partName get() = structure.partName ; set(value) { if( value != structure.partName) replaceStructure(structure.copy(partName = value), 2)}
        var alpha get() = structure.alpha ; set(value) { if( value != structure.alpha) replaceStructure(structure.copy(alpha = value), 3)}
        var transX get() = structure.transX ; set(value) { if( value != structure.transX) replaceStructure(structure.copy(transX = value), 4)}
        var transY get() = structure.transY ; set(value) { if( value != structure.transY) replaceStructure(structure.copy(transY = value), 5)}
        var scaleX get() = structure.scaleX ; set(value) { if( value != structure.scaleX) replaceStructure(structure.copy(scaleX = value), 6)}
        var scaleY get() = structure.scaleY ; set(value) { if( value != structure.scaleY) replaceStructure(structure.copy(scaleY = value), 7)}
        var rot get() = structure.rot ; set(value) { if( value != structure.rot) replaceStructure(structure.copy(rot = value), 8)}
        var centerX get() = structure.centerX ; set(value) { if( value != structure.centerX) replaceStructure(structure.copy(centerX = value), 9)}
        var centerY get() = structure.centerY ; set(value) { if( value != structure.centerY) replaceStructure(structure.copy(centerY = value), 10)}

        private fun replaceStructure( newStructure: SpritePartStructure, structureCode: Int) {
            if( structure != newStructure)
                undoEngine.performAndStore(SpriteStructureAction(newStructure, structureCode))
        }

        inner class SpriteStructureAction(
                private var newStructure: SpritePartStructure,
                private val structureCode: Int
        ) : NullAction(), StackableAction {
            private val oldStructure: SpritePartStructure = structure
            override val description: String get() = "Change Part Structure"
            override fun performAction() {
                structure = newStructure
                _sort()
                triggerChange()
            }
            override fun undoAction() {
                structure = oldStructure
                _sort()
                triggerChange()
            }

            override fun canStack(other: UndoableAction) =
                    (other is SpriteStructureAction) && other._context == _context && other.structureCode == structureCode
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

