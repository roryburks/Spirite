package spirite.base.imageData.layers.sprite

import rb.extendo.dataStructures.SinglyList
import rb.extendo.dataStructures.SinglySequence
import rb.extendo.extensions.toHashMap
import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.linear.MutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.ceil
import rb.vectrix.mathUtil.floor
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.*
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.drawer.DefaultImageDrawer
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.traverse
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.imageData.undo.UndoableDelegate
import spirite.base.util.StringUtil
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType

/**
 *  A SpriteLayer is a collection of Dynamic Mediums with various offsets, transforms, and
 */
class SpriteLayer : Layer {

    constructor(
            workspace: IImageWorkspace,
            mediumRepo: MMediumRepository)
    {
        this.workspace = workspace
        this.mediumRepo = mediumRepo
        _parts.add(SpritePart(SpritePartStructure(0, "base"), mediumRepo.addMedium(DynamicMedium(workspace, mediumRepo = mediumRepo)), workingId++))
        activePart = getAllLinkedLayers().firstOrNull()?.activePart?.partName?.run { parts.firstOrNull { it.partName == this}} ?: parts.firstOrNull()
    }

    constructor(
            workspace: IImageWorkspace,
            mediumRepo: MMediumRepository,
            toImport: List<Pair<MediumHandle,SpritePartStructure>>)
    {
        this.workspace = workspace
        this.mediumRepo = mediumRepo
        toImport.forEach {_parts.add(SpritePart(it.second, it.first, workingId++))}
        _sort()
        activePart = getAllLinkedLayers().firstOrNull()?.activePart?.partName?.run { parts.firstOrNull { it.partName == this}} ?: parts.firstOrNull()
    }

    constructor(
            toImport : List<SpritePartStructure>,
            workspace: IImageWorkspace,
            mediumRepo: MMediumRepository)
    {
        this.workspace = workspace
        this.mediumRepo = mediumRepo
        toImport.forEach {
            _parts.add(SpritePart(it, mediumRepo.addMedium(DynamicMedium(workspace, mediumRepo = mediumRepo)), workingId++))
        }
        _sort()
        activePart = getAllLinkedLayers().firstOrNull()?.activePart?.partName?.run { parts.firstOrNull { it.partName == this}} ?: parts.firstOrNull()
    }

    val workspace: IImageWorkspace
    val mediumRepo: MMediumRepository
    val undoEngine get() = workspace.undoEngine
    val parts : List<SpritePart> get() = _parts
    private val _parts = mutableListOf<SpritePart>()
    private var workingId = 0

    var activePartBind = Bindable<SpritePart?>(null)
            .also { it.addObserver(false) { new, _ ->
                cAlphaBind.field = new?.alpha ?: 1f
                cDepthBind.field = new?.depth ?: 0
                cVisibleBind.field = new?.visible ?: true
                cPartNameBind.field = new?.partName ?: ""
                cTransXBind.field = new?.transX ?: 0f
                cTransYBind.field = new?.transY ?: 0f
                cScaleXBind.field = new?.scaleX ?: 1f
                cScaleYBind.field = new?.scaleY ?: 1f
                cRotBind.field = new?.rot ?: 0f

                val name = new?.partName
                if( name != null) {
                    getAllLinkedLayers()
                            .forEach { sprite -> sprite.activePart = sprite.parts.firstOrNull { it.partName == name }  ?: sprite.activePart}
                }
            } }
    var activePart : SpritePart? by activePartBind

    private var _layerChangeObserver = Observable<()->Any?>()
    val layerChangeObserver : IObservable<()->Any?> get() = _layerChangeObserver
    private fun triggerChange() {
        _layerChangeObserver.trigger { it() }
        workspace.imageObservatory.triggerRefresh(
                ImageChangeEvent(
                        emptyList(),
                        workspace.groupTree.root.getAllNodesSuchThat({(it as? LayerNode)?.layer == this}),
                        workspace)
        )
    }



    // region ILayer methods

//    private val _keyPointsDerived = DerivedLazy{ etc }
    private val _keyPoints get() = parts.flatMap { part ->
        val tPartToWhole = part.tPartToWhole
                listOf(
                        Vec2f(0f,0f),
                        Vec2f(0f, part.handle.height+0f),
                        Vec2f( part.handle.width+0f,0f),
                        Vec2f(part.handle.width+0f, part.handle.height+0f))
                        .map { tPartToWhole.apply(it)}
    }

    override val x: Int get() = _keyPoints.map { it.xf.floor }.min() ?: 0
    override val y: Int get() = _keyPoints.map { it.yf.floor }.min() ?: 0

    override val width: Int get() {
        val xs = _keyPoints.map { it.xf.ceil }
        return (xs.max() ?: 0) - (xs.min() ?: 0)
    }
    override val height: Int get() {
        val ys = _keyPoints.map { it.yf.ceil }
        return (ys.max() ?: 0) - (ys.min() ?: 0)
    }

    override val activeData: ArrangedMediumData
        get() {
        val part = activePart ?: parts.first()
        return ArrangedMediumData( part.handle, part.tPartToWhole)
    }

    override fun getDrawer(arranged: ArrangedMediumData) = DefaultImageDrawer(arranged)
    override val imageDependencies: List<MediumHandle> get() = parts.map { it.handle }
    override fun getDrawList(isolator: IIsolator?): List<TransformedHandle> {
        return when (isolator) {
            is ISpriteLayerIsolator -> parts
                    .filter { it.isVisible }
                    .mapNotNull {
                        val subIsolator = isolator.getIsolationForPart(it)
                        val rubric = subIsolator.rubric
                        when {
                            !subIsolator.isDrawn -> null
                            rubric == null -> TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha)
                            else -> TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha).stack(rubric)
                        }
                    }
            else -> parts
                    .filter { it.isVisible }
                    .map { TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha) }
        }
    }

    override fun dupe(mediumRepo: MMediumRepository) =
            SpriteLayer( workspace, mediumRepo, parts.map { Pair(mediumRepo.addMedium(it.handle.medium.dupe()), SpritePartStructure(it)) })
    // endregion


    // region Part Add/Move/Remove
    fun addPartLinked(partName: String, depth: Int? = null)
    {
        val linked = getAllLinkedLayers()
        val names = linked.flatMap { sprite -> sprite.parts.asSequence().map { it.partName } }.distinct().toSet()
        val realName = StringUtil.getNonDuplicateName(names, partName)

        val aPart = activePart
        val realDepth = depth ?: when {
            !_parts.any() -> 0
            aPart == null -> _parts.last().depth + 1
            else -> aPart.depth+1
        }

        undoEngine.doAsAggregateAction("Add Sprite Part (Linked)") {
            linked.forEach {it.addPart(realName, realDepth)}
        }
    }
    fun addPart( partName : String, depth: Int? = null) {
        val handle = mediumRepo.addMedium( DynamicMedium(workspace, mediumRepo = mediumRepo))

        val aPart = activePart
        val realDepth = depth ?: when {
            !_parts.any() -> 0
            aPart == null -> _parts.last().depth + 1
            else -> aPart.depth+1
        }

        undoEngine.doAsAggregateAction("Add Sprite Part") {

            if(aPart != null) {
                val remapping = _parts.map { Pair(it,it.depth) }.toMap().toMutableMap()
                _bubbleUpDepth(
                        realDepth+1,
                        _parts.asSequence().drop(_parts.indexOf(aPart)+1),
                        remapping)
                undoEngine.performAndStore(DepthRemappingAction(remapping.toList()))
            }
            _addPart( SpritePartStructure( realDepth, partName), handle)
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
        if( fromIndex == toIndex) return

        val remapping = _parts.map { Pair(it,it.depth) }.toMap().toMutableMap()

        val toMove = _parts[fromIndex]
        val leftIndex = if( toIndex > fromIndex) toIndex else toIndex-1
        val rightIndex = if( toIndex < fromIndex) toIndex else toIndex+1
        val left = _parts.getOrNull(leftIndex)
        val right = _parts.getOrNull(rightIndex)

        when {
            left == null -> remapping[toMove] = _parts.first().depth - 1
            right == null -> remapping[toMove] = parts.last().depth + 1
            left.depth == right.depth || left.depth == right.depth -1 -> {
                remapping[toMove] = left.depth + 1
                _bubbleUpDepth(
                        left.depth + 2,
                        _parts.asSequence().drop(leftIndex).filter { it != toMove },
                        remapping)
            }
            else -> remapping[toMove] = left.depth + 1
        }

        undoEngine.performAndStore(DepthRemappingAction(remapping.toList()))
    }

    private fun _bubbleUpDepth( startDepth: Int, parts: Sequence<SpritePart>, mapping: MutableMap<SpritePart,Int>) {
        var currentDepth = startDepth
        parts.forEach {
            if( mapping[it]!! < currentDepth) {
                mapping[it] = currentDepth
            }
            currentDepth = (mapping[it]!! ) + 1
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
        _parts.sortWith(compareBy({it.depth}, {it.subdepth}))
    }


    inner class DepthRemappingAction( val remap: List<Pair<SpritePart,Int>>)
        : NullAction()
    {
        val reverseMap = remap.map { (part, _) -> Pair(part, part.structure.depth) }
        override val description: String get() = "Sprite Part Depth Change"
        override fun performAction() {
            remap.forEach { (from,to) ->from.structure = from.structure.copy(depth = to)}
            _sort()
        }

        override fun undoAction() {
            reverseMap.forEach { (from,to) ->from.structure = from.structure.copy(depth = to)}
            _sort()
        }
    }
    // endregion


    val cDepthBind = Bindable(0)  .also{it.addObserver { new, _ -> activePart?.depth = new }}
    val cVisibleBind = Bindable(true) .also{it.addObserver { new, _ -> activePart?.visible = new }}
    val cPartNameBind = Bindable("") .also{it.addObserver { new, _ -> activePart?.partName = new }}
    val cAlphaBind = Bindable(1f) .also { it.addObserver { new, _ -> activePart?.alpha = new  } }
    val cTransXBind = Bindable(0f) .also{it.addObserver { new, _ -> activePart?.transX = new }}
    val cTransYBind = Bindable(0f).also{it.addObserver { new, _ -> activePart?.transY = new }}
    val cScaleXBind = Bindable(1f).also{it.addObserver { new, _ -> activePart?.scaleX = new }}
    val cScaleYBind = Bindable(1f).also{it.addObserver { new, _ -> activePart?.scaleY = new }}
    val cRotBind = Bindable(0f).also{it.addObserver { new, _ -> activePart?.rot = new }}

    private fun getAllLinkedLayers() : Sequence<SpriteLayer> {
        return workspace.groupTree.root.traverse()
                .firstOrNull { (it as? LayerNode)?.layer == this@SpriteLayer }
                ?.parent?.children?.asSequence()
                ?.mapNotNull { ((it as? LayerNode)?.layer as? SpriteLayer) }
                ?: SinglySequence(this)
    }

    inner class SpritePart(
            _structure: SpritePartStructure,
            val handle: MediumHandle,
            subdepth: Int)
    {
        internal var structure = _structure
        val context get() = this@SpriteLayer

        var subdepth by UndoableDelegate(subdepth, workspace.undoEngine, "Internal Change (should not see this).")

        var depth get() = structure.depth ; set(value)
        {
        }

        // region Shadowing SpritePartStructure scroll with Undoable Wrapper
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
            if( structure != newStructure) {
                val linked = getAllLinkedLayers()
                        .mapNotNull { sprite -> sprite.parts.firstOrNull { it.partName == partName } }
                        .toList()

                undoEngine.doAsAggregateAction("Change Sprite Part Structure") {
                    linked.forEach { undoEngine.performAndStore(it.SpriteStructureAction(newStructure, structureCode)) }
                }
            }
        }

        private fun refreshBinds()
        {
            if( activePart == this) {
                cAlphaBind.field = alpha
                cDepthBind.field = depth
                cVisibleBind.field = visible
                cPartNameBind.field = partName
                cAlphaBind.field = alpha
                cTransXBind.field = transX
                cTransYBind.field =transY
                cScaleXBind.field =scaleX
                cScaleYBind.field = scaleY
                cRotBind.field = rot
            }
        }

        inner class SpriteStructureAction(
                private var newStructure: SpritePartStructure,
                private val structureCode: Int)
            : NullAction(), StackableAction
        {
            private val oldStructure: SpritePartStructure = structure
            override val description: String get() = "Change Part Structure"
            override fun performAction() {
                structure = newStructure
                _sort()
                refreshBinds()
                triggerChange()
            }
            override fun undoAction() {
                structure = oldStructure
                _sort()
                refreshBinds()
                triggerChange()
            }

            override fun canStack(other: UndoableAction) =
                    (other is SpriteStructureAction) && other._context == _context && other.structureCode == structureCode
            override fun stackNewAction(other: UndoableAction) {newStructure = (other as SpriteStructureAction).newStructure}
            private val _context: SpriteLayer get() = this@SpriteLayer
        }
        // endregion

        val isVisible: Boolean get() = structure.visible && structure.alpha != 0f
        val tPartToWhole : MutableTransformF
            get() {
            val cx = structure.centerX?.toFloat() ?: handle.width/2f
            val cy = structure.centerY?.toFloat() ?: handle.height/2f
            val ret = MutableTransformF.Translation(-cx, -cy)
            ret.preScale(structure.scaleX, structure.scaleY)
            ret.preRotate( structure.rot)
            ret.preTranslate( structure.transX + handle.width/2f, structure.transY + handle.height/2f)
            return  ret
        }

        val tWholeToPart : MutableTransformF get() = tPartToWhole.invert()?.toMutable() ?: MutableTransformF.Identity

    }
}

