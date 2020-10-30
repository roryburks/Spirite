package spirite.base.imageData

import rb.global.IContract
import rb.extendo.dataStructures.SinglyList
import rb.owl.GuardedObservable
import rb.owl.IObservable
import rb.owl.Observer
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.brains.palette.paletteSwapDriver.IPaletteMediumMap
import spirite.base.brains.palette.paletteSwapDriver.PaletteMediumMap
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.isolation.IIsolationManager
import spirite.base.graphics.isolation.IsolationManager
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.animation.AnimationManager
import spirite.base.imageData.animation.IAnimationManager
import spirite.base.imageData.animationSpaces.AnimationSpaceManager
import spirite.base.imageData.animationSpaces.IAnimationSpaceManager
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.MultiMediumDrawer
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.mediumGroups.IMediumGroupRepository
import spirite.base.imageData.mediumGroups.MediumGroupRepository
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.Compositor
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.imageData.selection.ISelectionEngine.SelectionChangeEvent
import spirite.base.imageData.selection.SelectionEngine
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoEngine
import spirite.base.imageData.undo.UndoableDelegate
import spirite.base.imageData.view.IViewSystem
import spirite.base.imageData.view.ViewSystem
import spirite.base.pen.stroke.IStrokeDrawerProvider
import java.io.File

interface IImageWorkspace {
    var width: Int
    var height: Int

    // File-Related
    fun finishBuilding()
    fun fileSaved( newFile: File)
    val file: File?
    val filename get() = file?.name ?: "Untitled Image"
    val hasChanged: Boolean
    val displayedFilenameBind : IBindable<String>



	// Sub-Components
    val groupTree: PrimaryGroupTree
    val mediumRepository : IMediumRepository
    val animationManager : IAnimationManager
    val referenceManager : IReferenceManager
    val isolationManager: IIsolationManager
    val animationSpaceManager : IAnimationSpaceManager
    val filterManager : IFilterManager

    val undoEngine : IUndoEngine
    val selectionEngine : ISelectionEngine
    val paletteSet : PaletteSet
    val viewSystem: IViewSystem
    val paletteMediumMap: IPaletteMediumMap
    val imageObservatory: IImageObservatory
    val compositor : Compositor
    val mediumGroupRepository: IMediumGroupRepository
//	public StagingManager getStageManager() {return stagingManager;}

    // Super-Components
    val renderEngine : IRenderEngine
    val settingsManager: ISettingsManager
    val paletteManager : IPaletteManager
    val strokeProvider : IStrokeDrawerProvider
    val toolset : Toolset

    // Root level stuff
    val activeMediumBind : IBindable<MediumHandle?>
    val activeData : ArrangedMediumData?
    fun arrangeActiveDataForNode( node: LayerNode) : ArrangedMediumData

    val activeDrawerObs: IObservable<()->Unit>
    fun triggerActiveDrawerChange() // wrong-minded but for now it's easier
    val activeDrawer : IImageDrawer
    val anchorDrawer: IImageDrawer
    fun getDrawerForNode( node: Node) : IImageDrawer


}

interface MImageWorkspace : IImageWorkspace
{
    override val mediumRepository : MMediumRepository
    override var hasChanged: Boolean

    fun triggerInternalMediumChanged()
}

class ImageWorkspace(
        override val renderEngine : IRenderEngine,
        override val settingsManager: ISettingsManager,
        override val paletteManager : IPaletteManager,
        override val strokeProvider: IStrokeDrawerProvider,
        override val toolset: Toolset,
        width: Int = 100,
        height: Int = 100) : MImageWorkspace
{
    override val mediumRepository = MediumRepository( this)
    override val undoEngine = UndoEngine(this, mediumRepository)
    override val imageObservatory: IImageObservatory = ImageObservatory()
    override val viewSystem: IViewSystem = ViewSystem(undoEngine)
    override val groupTree = PrimaryGroupTree(this) // Needs to be after ViewSystem, UndoEngine
    override val animationManager: IAnimationManager = AnimationManager(this)
    override val selectionEngine: ISelectionEngine
    override val referenceManager: ReferenceManager = ReferenceManager()
    override val paletteSet: PaletteSet = paletteManager.makePaletteSet()
    override val isolationManager: IIsolationManager = IsolationManager(this)
    override val animationSpaceManager: IAnimationSpaceManager = AnimationSpaceManager(this)
    override val filterManager: IFilterManager = FilterManager()
    override val paletteMediumMap: IPaletteMediumMap
    override val compositor = Compositor()
    override val mediumGroupRepository = MediumGroupRepository(this)

    override var width: Int by UndoableDelegate(width, undoEngine, "Changed Workspace Width")
    override var height: Int by UndoableDelegate(height, undoEngine, "Changed Workspace Height")


    // region File-Related
    override fun finishBuilding() {
        undoEngine.reset()
        hasChanged = false
        mediumRepository.locked = false

        mediumRepository.dataList.forEach {
            val medium = mediumRepository.getData(it)
            if( medium is MaglevMedium && medium.builtImage.base == null)
                medium.build(MediumHandle(this, it))
        }
    }

    override fun fileSaved(newFile: File) {
        undoEngine.setSaveSpot()
        file = newFile
        hasChanged = false
    }

    override var file: File? = null
        set(value) {
            field = value
            updateDisplayedFilename()
        }
    override var hasChanged: Boolean = false
        set(value) {
            field = value
            updateDisplayedFilename()
        }
    private fun updateDisplayedFilename() {
        if( hasChanged)
            displayedFilenameBind.field = (file?.name ?: "<New Worspace>") + "*"
        else
            displayedFilenameBind.field = (file?.name ?: "<New Worspace>")
    }
    override val displayedFilenameBind = Bindable("<New Worspace>")

    // endregion


    // region Active Stuff
    // TODO: Extract this stuff into it's own service

    private val currentNode get() = groupTree.selectedNode


    override val activeMediumBind = Bindable<MediumHandle?>(null)
    override val activeData: ArrangedMediumData? get() = (currentNode as? LayerNode)?.let { arrangeActiveDataForNode(it) }
    override fun arrangeActiveDataForNode(node: LayerNode): ArrangedMediumData {
        val layerData = node.layer.activeData
        return layerData.copy(tMediumToWorkspace =  node.tNodeToContext * layerData.tMediumToWorkspace)
    }

    // region ActiveDrawer Tracking
    override val activeDrawerObs : GuardedObservable<()->Unit> = GuardedObservable()
    private val _amK = activeMediumBind.addObserver { _, _ -> triggerActiveDrawerChange()}
    private val _aDrawTreeK = groupTree.treeObservable.addObserver(Observer(object : TreeObserver {
        override fun treeStructureChanged(evt: TreeChangeEvent) {}
        override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {
            if( groupTree.selectedNode == node)
                triggerActiveDrawerChange()
        }
    }))

    override fun triggerActiveDrawerChange() {
        activeDrawerObs.trigger { it() }
    }

    // endregion

    override val activeDrawer: IImageDrawer get() {
        val currentNode = currentNode
        val liftedData = selectionEngine.liftedData
        return when {
            currentNode == null -> NillImageDrawer
            !settingsManager.allowDrawOnInvisibleLayers && !currentNode.isVisible -> NillImageDrawer
            liftedData != null -> liftedData.getImageDrawer(this)
            currentNode is GroupNode -> buildDrawerForGroup(currentNode)
            currentNode is LayerNode -> currentNode.layer.getDrawer(arrangeActiveDataForNode(currentNode))
            else -> NillImageDrawer
        }
    }
    fun buildDrawerForGroup(group: GroupNode) : IImageDrawer {
        fun buildRec(sub: Node) : Iterable<IImageDrawer>{
            return when( sub) {
                is LayerNode -> {
                    val localTrans = getTransformForNode(sub)
                    sub.layer.allArrangedData
                            .map { it.copy( tMediumToWorkspace = localTrans * it.tMediumToWorkspace) }
                            .map { sub.layer.getDrawer(it) }
                }
                is GroupNode -> sub.children.flatMap { buildRec(it) }
                else -> listOf()
            }
        }

        return MultiMediumDrawer(buildRec(group).toList())
    }


    fun getTransformForNode(node: Node) : ITransformF{
        val preTrans = when(val parent = node.parent) {
            null -> ImmutableTransformF.Identity
            else -> getTransformForNode(parent)
        }
        return preTrans * node.tNodeToContext
    }

    override val anchorDrawer: IImageDrawer get() {
        (currentNode as? LayerNode)?.also { return it.layer.getDrawer(arrangeActiveDataForNode(it)) }
        return NillImageDrawer
    }

    override fun getDrawerForNode(node: Node) = when( node) {
            is LayerNode -> node.layer.getDrawer(arrangeActiveDataForNode(node))
            else -> NillImageDrawer
    }

    // endregion

    override fun triggerInternalMediumChanged() {
        activeMediumBind.field = (groupTree.selectedNode as? LayerNode)?.run { layer.activeData.handle }
    }

    // region Internal Cross-Component Listeners
    init {
        groupTree.selectedNodeBind.addObserver { new, old ->
            activeMediumBind.field = (new as? LayerNode)?.run { layer.activeData.handle }
        }
    }

    private val _treeObsK = groupTree.treeObservable.addObserver(
        object : TreeObserver {
            override fun treeStructureChanged(evt: TreeChangeEvent) {
                imageObservatory.triggerRefresh(ImageChangeEvent(evt.changedNodes.flatMap { it.imageDependencies }, evt.changedNodes, this@ImageWorkspace))
            }

            override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {
                imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), SinglyList(node), this@ImageWorkspace))
            }
        }.observer()
    )

    // endregion

    init {
        // The pit of order-dependency shame
        paletteMediumMap = PaletteMediumMap(this)
        selectionEngine = SelectionEngine(this)
    }

    private val _selectionK : IContract = selectionEngine.selectionChangeObserver.addObserver({ it: SelectionChangeEvent ->
        imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), emptySet(), this@ImageWorkspace, liftedChange = it.isLiftedChange))
    }.observer())
}