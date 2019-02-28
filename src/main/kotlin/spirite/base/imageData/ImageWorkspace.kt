package spirite.base.imageData

import rb.extendo.dataStructures.SinglyList
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.animation.AnimationManager
import spirite.base.imageData.animation.IAnimationManager
import spirite.base.imageData.animationSpaces.AnimationSpaceManager
import spirite.base.imageData.animationSpaces.IAnimationSpaceManager
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.Compositor
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.imageData.selection.ISelectionEngine.SelectionChangeEvent
import spirite.base.imageData.selection.SelectionEngine
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoEngine
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.util.delegates.UndoableDelegate
import java.io.File
import kotlin.system.measureTimeMillis

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
//	public StagingManager getStageManager() {return stagingManager;}

    // Super-Components
    val renderEngine : IRenderEngine
    val settingsManager: ISettingsManager
    val paletteManager : IPaletteManager
    val strokeProvider : IStrokeDrawerProvider
    val toolset : Toolset

    val activeMediumBind : IBindable<MediumHandle?>
    val activeData : ArrangedMediumData?
    fun arrangeActiveDataForNode( node: LayerNode) : ArrangedMediumData

    val activeDrawer : IImageDrawer
    val anchorDrawer: IImageDrawer
    fun getDrawerForNode( node: Node) : IImageDrawer

    val imageObservatory: IImageObservatory

    val compositor : Compositor
}

interface MImageWorkspace : IImageWorkspace
{
    override val mediumRepository : MMediumRepository
    override var hasChanged: Boolean
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
    override val groupTree = PrimaryGroupTree(this, mediumRepository)
    override val animationManager: IAnimationManager = AnimationManager(this)
    override val selectionEngine: ISelectionEngine = SelectionEngine(this)
    override val referenceManager: ReferenceManager = ReferenceManager()
    override val paletteSet: PaletteSet = paletteManager.makePaletteSet()
    override val isolationManager: IIsolationManager = IsolationManager(this)
    override val animationSpaceManager: IAnimationSpaceManager = AnimationSpaceManager(this)
    override val filterManager: IFilterManager = FilterManager()

    override val compositor = Compositor()

    override var width: Int by UndoableDelegate(width, undoEngine, "Changed Workspace Width")
    override var height: Int by UndoableDelegate(height, undoEngine, "Changed Workspace Height")


    // region File-Related
    override fun finishBuilding() {
        undoEngine.reset()
        hasChanged = false
        mediumRepository.locked = false

        mediumRepository.dataList.forEach {
            val medium = mediumRepository.getData(it)
            if( medium is MaglevMedium)
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

    private val currentNode get() = groupTree.selectedNode


    override val activeMediumBind = Bindable<MediumHandle?>(null)
    override val activeData: ArrangedMediumData? get() = (currentNode as? LayerNode)?.let { arrangeActiveDataForNode(it) }
    override fun arrangeActiveDataForNode(node: LayerNode): ArrangedMediumData {
        val layerData = node.layer.activeData
        return layerData.copy(tMediumToWorkspace =  node.tNodeToContext * layerData.tMediumToWorkspace)
    }

    override val activeDrawer: IImageDrawer get() {
        selectionEngine.liftedData?.also { return it.getImageDrawer(this) }
        (currentNode as? LayerNode)?.also { return it.layer.getDrawer(arrangeActiveDataForNode(it)) }
        return NillImageDrawer
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

    private val _selectionK = selectionEngine.selectionChangeObserver.addObserver({ it: SelectionChangeEvent ->
        imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), emptySet(), this@ImageWorkspace, liftedChange = it.isLiftedChange))
    }.observer())
    // endregion
}