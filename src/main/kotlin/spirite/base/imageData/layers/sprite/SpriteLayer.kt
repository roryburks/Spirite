package spirite.base.imageData.layers.sprite

import rb.extendo.dataStructures.SinglyList
import rb.extendo.dataStructures.SinglySequence
import rb.extendo.delegates.OnChangeDelegate
import rb.extendo.extensions.toHashMap
import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.linear.MutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.ceil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.brains.DBGlobal
import spirite.base.graphics.drawer.DefaultImageDrawer
import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.graphics.drawer.MultiMediumDrawer
import spirite.base.graphics.drawer.NillImageDrawer
import spirite.base.graphics.isolation.IIsolator
import spirite.base.graphics.isolation.ISpriteLayerIsolator
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.TransformedHandle
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.traverse
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.MediumType
import spirite.base.imageData.mediums.magLev.MaglevImageDrawer
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.imageData.undo.UndoableDelegate
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug.WarningType
import spirite.core.util.StringUtil

/**
 *  A SpriteLayer is a collection of Dynamic Mediums with various offsets, transforms, and
 */
class SpriteLayer : Layer {

    val type : MediumType

    constructor(workspace: MImageWorkspace, type: MediumType = MediumType.DYNAMIC)
    {
        this.type = type
        this.workspace = workspace
        _parts.add(SpritePart(SpritePartStructure(0, "base"), workspace.mediumRepository.addMedium(makeThing(workspace)), _workingId++))
        activePart = _parts.first()
    }

    constructor(
            workspace: MImageWorkspace,
            toImport: List<Pair<MediumHandle,SpritePartStructure>>,
            type: MediumType = MediumType.DYNAMIC)
    {
        this.type = type
        this.workspace = workspace
        toImport.forEach {_parts.add(SpritePart(it.second, it.first, _workingId++))}
        _sort()
        activePart = _parts.first()
    }

    constructor(
            toImport : List<SpritePartStructure>,
            workspace: MImageWorkspace,
            type: MediumType = MediumType.DYNAMIC)
    {
        this.type = type
        this.workspace = workspace
        toImport.forEach {
            _parts.add(SpritePart(it, workspace.mediumRepository.addMedium(makeThing(workspace)), _workingId++))
        }
        _sort()
        activePart = _parts.first()
    }

    lateinit var workspace: MImageWorkspace ; private set
    val undoEngine get() = workspace.undoEngine
    /** Gets a list of SpriteParts that are pre-sorted by Depth */
    val parts : List<SpritePart> get() = _parts
    private val _parts = mutableListOf<SpritePart>()
    private var _workingId = 0

    var multiSelect by OnChangeDelegate<Set<SpritePart>?>(null) {
        if( it != null) {
            workspace.triggerActiveDrawerChange()
            triggerChange()
        }
    }
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
                multiSelect = null

                // For all Linked SpriteLayes, also set the active part to the one selected.
                val name = new?.partName
                if( name != null) {
                    getAllLinkedLayers()
                            .forEach { sprite -> sprite.activePart = sprite.parts.firstOrNull { it.partName == name }  ?: sprite.activePart}
                }
                triggerChange()
            }}
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
        workspace.triggerInternalMediumChanged()
    }

    fun makeThing(workspace: MImageWorkspace) = when(type) {
        MediumType.DYNAMIC -> {
            DynamicMedium(workspace)
        }
        MediumType.MAGLEV -> MaglevMedium(workspace)
        else -> TODO("Unimplemented.")
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
    override val allArrangedData: List<ArrangedMediumData> get() = parts.map { ArrangedMediumData(it.handle, it.tPartToWhole) }

    override fun getDrawer(arranged: ArrangedMediumData) : IImageDrawer {
        val multiSelect = multiSelect

        return when {
            multiSelect != null && multiSelect.count() > 2-> buildMultiDrawer(multiSelect, arranged)
            else -> when(val med = arranged.handle.medium) {
                is MaglevMedium -> MaglevImageDrawer(arranged, med)
                is DynamicMedium -> DefaultImageDrawer(arranged)
                else -> NillImageDrawer
            }
        }
    }

    private fun buildMultiDrawer( set: Set<SpritePart>, original: ArrangedMediumData) =
            MultiMediumDrawer(
                    set.map { getDrawer(it, original) },
                    cx = set.map { it.handle.x + it.handle.width/2f }.average().f,
                    cy = set.map { it.handle.y + it.handle.height/2f }.average().f)
    private fun getDrawer(part: SpritePart, original: ArrangedMediumData) : IImageDrawer {
        val arranged =
                if( part.handle == original.handle) original
                else ArrangedMediumData(part.handle, part.tPartToWhole) // TODO: Do this better (find the mutating transform and apply)

        return when (val med = arranged.handle.medium) {
            is MaglevMedium -> MaglevImageDrawer(arranged, med)
            is DynamicMedium -> DefaultImageDrawer(arranged)
            else -> NillImageDrawer
        }
    }

    override val imageDependencies: List<MediumHandle> get() = parts.map { it.handle }
    override fun getDrawList(isolator: IIsolator?): List<TransformedHandle> {
        val base = parts
            .filter { it.isVisible }
            .filter { !DBGlobal.filterSet.contains(it.partName.first()) }

        return when (isolator) {
            is ISpriteLayerIsolator -> base
                    .mapNotNull {
                        val subIsolator = isolator.getIsolationForPart(it)
                        val rubric = subIsolator.rubric
                        when {
                            !subIsolator.isDrawn -> null
                            rubric == null -> TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha)
                            else -> TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha).stack(rubric)
                        }
                    }
            else -> base
                .map { TransformedHandle(it.handle, it.depth, it.tPartToWhole, it.alpha) }
        }
    }

    override fun dupe(workspace: MImageWorkspace) =
            SpriteLayer( workspace, parts.map { Pair(workspace.mediumRepository.addMedium(it.handle.medium.dupe(workspace)), SpritePartStructure(it)) }, type)
    // endregion

    // region Part Add/Insert/Move/Remove
    /***
     * Adds the PartName as a new Part (pre-vetted that its part-name does not conflict with any existing part names).
     * It makes sure depths do not overlap, by incrementing depths if necessary
     */
    private fun addPartDirect( partName: String, depth: Int? = null) {
        val handle = workspace.mediumRepository.addMedium(makeThing(workspace))

        val aPart = activePart
        val realDepth = depth ?: when {
            !_parts.any() -> 0
            aPart == null -> _parts.last().depth + 1
            else -> aPart.depth + 1
        }

        if (aPart != null) {
            val remapping = _parts.toHashMap({it.partName}, {it.depth})
            _bubbleUpDepth(
                realDepth + 1,
                _parts.asSequence().drop(_parts.indexOf(aPart) + 1),
                remapping)
            undoEngine.performAndStore(DepthRemappingAction(remapping, _parts.toHashMap({it.partName}, {it.depth})))
        }
        _addPart(SpritePartStructure(realDepth, partName), handle)
    }

    /***
     * Creates a new Part.
     */
    enum class SpritePartAddMode {
        CreateDisjoint,
        CreateLinked,
        CreateIfAbsent
    }
    fun addPart(partName: String, depth: Int? = null, mode: SpritePartAddMode = SpritePartAddMode.CreateDisjoint)
    {
        undoEngine.doAsAggregateAction("Add Sprite Part") {
            when( mode) {
                SpritePartAddMode.CreateLinked -> {
                    val incompatibleNames = getAllLinkedLayers()
                        .flatMap { sprite -> sprite.parts.asSequence()
                            .map { it.partName } }
                        .toHashSet()
                    val realName = StringUtil.getNonDuplicateName(incompatibleNames, partName)
                    getAllLinkedLayers()
                        .forEach { it.addPartDirect(realName, depth) }
                    activePart = _parts.firstOrNull { it.partName == realName } ?: activePart
                }
                SpritePartAddMode.CreateDisjoint -> {
                    val incompatibleNames = getAllLinkedLayers()
                        .flatMap { sprite -> sprite.parts.asSequence()
                            .map { it.partName } }
                        .toHashSet()
                    val realName = StringUtil.getNonDuplicateName(incompatibleNames, partName)
                    addPartDirect(realName, depth)
                    activePart = _parts.firstOrNull { it.partName == realName } ?: activePart
                }
                SpritePartAddMode.CreateIfAbsent -> {
                    if( !_parts.any{ it.partName == partName}) {
                        addPartDirect(partName, depth)
                        activePart = _parts.firstOrNull { it.partName == partName } ?: activePart
                    }
                }
            }
        }

    }

    fun insertPart( handle: MediumHandle, structure: SpritePartStructure) {
        _addPart(structure, handle)
    }

    fun removePart( toRemove: SpritePart, linked: Boolean = false) : Boolean  {
        fun removeSub( layer: SpriteLayer, part: SpritePart) : Boolean{
            layer.run {
                if( !_parts.contains(part) || _parts.count() <= 1)
                    return false
                else {
                    undoEngine.performAndStore(object : NullAction() {
                        override val description: String get() = "Remove Sprite Part"

                        override fun performAction() {
                            if (activePart == part)
                                activePart = null
                            _parts.remove(part)
                            triggerChange()
                        }

                        override fun undoAction() {
                            // Note: Since _parts are automatically sorted by drawDepth, no need to remember its old drawDepth
                            _parts.add(part)
                            _sort()
                            triggerChange()
                        }

                        override fun getDependencies() = SinglyList(part.handle)
                    })
                    return true
                }
            }
        }

        return if( linked) {
            var hasChanged = false
            undoEngine.doAsAggregateAction("Removing Sprite Part [Linked]") {
                hasChanged = getAllLinkedLayers()
                    .map { layer -> layer._parts
                        .firstOrNull { it.partName == toRemove.partName }
                        ?.run { removeSub(layer,this) } ?: false
                    }
                    .any()
            }
            hasChanged
        } else
            removeSub(this, toRemove)
    }

    fun movePart( fromIndex: Int, toIndex: Int) {
        if( fromIndex == toIndex) return

        val remapping = _parts.toHashMap({it.partName}, {it.depth})

        val toMove = _parts[fromIndex]
        val leftIndex = if( toIndex > fromIndex) toIndex else toIndex-1
        val rightIndex = if( toIndex < fromIndex) toIndex else toIndex+1
        val left = _parts.getOrNull(leftIndex)
        val right = _parts.getOrNull(rightIndex)

        when {
            left == null -> remapping[toMove.partName] = _parts.first().depth - 1
            right == null -> remapping[toMove.partName] = parts.last().depth + 1
            left.depth == right.depth || left.depth == right.depth -1 -> {
                remapping[toMove.partName] = left.depth + 1
                _bubbleUpDepth(
                        left.depth + 2,
                        _parts.asSequence().drop(leftIndex).filter { it != toMove },
                        remapping)
            }
            else -> remapping[toMove.partName] = left.depth + 1
        }

        undoEngine.performAndStore(DepthRemappingAction(remapping, _parts.toHashMap({it.partName}, {it.depth})))
    }

    private fun _bubbleUpDepth( startDepth: Int, parts: Sequence<SpritePart>, mapping: MutableMap<String,Int>) {
        var currentDepth = startDepth
        parts.forEach {
            if( mapping[it.partName]!! < currentDepth) {
                mapping[it.partName] = currentDepth
            }
            currentDepth = (mapping[it.partName]!! ) + 1
        }
    }

    private fun _addPart( structure: SpritePartStructure, handle: MediumHandle) {
        if( _parts.size >= 255) {
            // Primarily for save/load simplicity
            DebugProvider.debug.handleWarning(WarningType.UNSUPPORTED, "Only 255 parts per rig currently supported.")
            return
        }

        val name = StringUtil.getNonDuplicateName( _parts.map { it.partName }, structure.partName)
        val newStructure = structure.copy(partName =  name)
        val toAdd = SpritePart(newStructure, handle, _workingId++)


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


    // endregion

    // region Weird Commands
    fun replaceMedium(partName: String, newMedium: MediumHandle) {
        val existingPart = _parts.firstOrNull { it.partName == partName } ?: return

        removePart(existingPart, false)
        _addPart(existingPart.structure, newMedium)
    }

    // region Depth Remapping
    fun remapDepth(newDepths: Map<SpritePart,Int>)
    {
        val oldMap = _parts
                .map {Pair(it.partName, it.depth)}
                .toMap()
        val newMap = _parts
                .map {Pair(it.partName, newDepths[it] ?: it.depth)}
                .toMap()
        undoEngine.performAndStore(DepthRemappingAction(newMap, oldMap))
    }

    inner class DepthRemappingAction(
            val newMap: Map<String,Int>,
            val oldMap: Map<String,Int>) : NullAction()
    {
        override val description: String get() = "Sprite Part Depth Change"
        override fun performAction() {
            getAllLinkedLayers().forEach { sprite ->
                sprite._parts.forEach { newMap[it.partName]?.apply { it.structure = it.structure.copy(depth = this) } }
                sprite._sort()
                sprite.triggerChange()
            }
        }

        override fun undoAction() {
            getAllLinkedLayers().forEach { sprite ->
                sprite._parts.forEach { oldMap[it.partName]?.apply { it.structure = it.structure.copy(depth = this) } }
                sprite._sort()
                sprite.triggerChange()
            }
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

    fun getAllLinkedLayers() : Sequence<SpriteLayer> {
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
            if( structure.depth != value) {
                val remapping = _parts.toHashMap({it.partName}, {it.depth})
                remapping[partName] = value
                _bubbleUpDepth(
                        value + 1,
                        _parts.asSequence().filter { it.depth >= value && it != this},
                        remapping)
                undoEngine.performAndStore(DepthRemappingAction(remapping,_parts.toHashMap({it.partName}, {it.depth})))
            }
        }

        var partName
            get() = structure.partName
            set(value) {
                if( value != structure.partName) {
                    // Avoid Creating a dupe name as that is a key.  Note: we purposefully do not check for nondupe
                    // across linked sprites because that is a valid action(it creates the link)
                    val currentNames = parts
                            .filter { it != this }
                            .map { it.partName }
                    val nondupeName = StringUtil.getNonDuplicateName(currentNames, value)
                    replaceStructure(structure.copy(partName = nondupeName), 2)
                }

            }

        // region Shadowing SpritePartStructure scroll with Undoable Wrapper
        var visible get() = structure.visible ; set(value) { if( value != structure.visible) replaceStructure(structure.copy(visible = value), 1)}
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

