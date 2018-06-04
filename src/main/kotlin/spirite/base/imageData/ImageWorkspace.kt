package spirite.base.imageData

import spirite.base.brains.Bindable
import spirite.base.brains.IBindable
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.animation.AnimationManager
import spirite.base.imageData.animation.IAnimationManager
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.Compositor
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.imageData.selection.ISelectionEngine.SelectionChangeEvent
import spirite.base.imageData.selection.SelectionEngine
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoEngine
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.util.delegates.UndoableDelegate
import spirite.base.util.groupExtensions.SinglyList
import spirite.base.util.groupExtensions.mapAggregated
import spirite.pc.master
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



	// Sub-Components
    val groupTree: PrimaryGroupTree
    val mediumRepository : IMediumRepository
    val animationManager : IAnimationManager
    val referenceManager : IReferenceManager

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
}

class ImageWorkspace(
        override val renderEngine : IRenderEngine,
        override val settingsManager: ISettingsManager,
        override val paletteManager : IPaletteManager,
        override val strokeProvider: IStrokeDrawerProvider,
        width: Int = 100,
        height: Int = 100) : MImageWorkspace
{
    override val mediumRepository = MediumRepository( this)
    override val undoEngine = UndoEngine(this, mediumRepository)
    override val imageObservatory: IImageObservatory = ImageObservatory()
    override val groupTree = PrimaryGroupTree(this, mediumRepository)
    override val animationManager: IAnimationManager get() = AnimationManager(this)
    override val selectionEngine: ISelectionEngine = SelectionEngine(this)
    override val referenceManager: ReferenceManager = ReferenceManager()
    override val paletteSet: PaletteSet get() = TODO("not implemented")
    override val toolset: Toolset get() = master.toolsetManager.toolset

    override val compositor = Compositor()

    override var width: Int by UndoableDelegate(width, undoEngine, "Changed Workspace Width")
    override var height: Int by UndoableDelegate(height, undoEngine, "Changed Workspace Height")


    override fun finishBuilding() {
        undoEngine.reset()
        hasChanged = false
        mediumRepository.locked = false
    }

    override fun fileSaved(newFile: File) {
        file = newFile
        hasChanged = false
    }

    override var file: File? = null
    override var hasChanged: Boolean = false

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


    // Add internal component-to-component event triggers
    init {
        groupTree.selectedNodeBind.addListener { new, old ->
            activeMediumBind.field = (new as? LayerNode)?.run { layer.activeData.handle }
        }
    }

    private val treeListener = object : TreeObserver {
        override fun treeStructureChanged(evt: TreeChangeEvent) {
            imageObservatory.triggerRefresh(ImageChangeEvent(evt.changedNodes.mapAggregated { it.imageDependencies }, evt.changedNodes, this@ImageWorkspace))
        }

        override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {
            imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), SinglyList(node), this@ImageWorkspace))
        }
    }.apply { groupTree.treeObs.addObserver(this)}

    private val selectionListener : (SelectionChangeEvent)->Unit = { it : SelectionChangeEvent ->
        imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), emptySet(), this@ImageWorkspace, liftedChange = it.isLiftedChange))
    }.also { obs -> selectionEngine.selectionChangeObserver.addObserver(obs) }
}