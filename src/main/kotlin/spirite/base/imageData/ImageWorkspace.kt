package spirite.base.imageData

import spirite.base.brains.Bindable
import spirite.base.brains.IBindable
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.Compositor
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.imageData.mediums.drawer.NillImageDrawer
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoEngine
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.util.delegates.UndoableDelegate
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

    val activeData : ArrangedMediumData?
    fun arrangeActiveDataForNode( node: LayerNode) : ArrangedMediumData

    val activeDrawerBind: IBindable<IImageDrawer>
    val activeDrawer : IImageDrawer
    fun getDrawerForNode( node: Node) : IImageDrawer

    val imageObservatory: IImageObservatory

    val compositor : Compositor
}

class ImageWorkspace(
        override val renderEngine : IRenderEngine,
        override val settingsManager: ISettingsManager,
        override val paletteManager : IPaletteManager,
        override val strokeProvider: IStrokeDrawerProvider,
        width: Int = 100,
        height: Int = 100) : IImageWorkspace
{
    override val mediumRepository = MediumRepository( this)
    override val undoEngine = UndoEngine(this, mediumRepository)
    override val imageObservatory: IImageObservatory = ImageObservatory()
    override val groupTree = PrimaryGroupTree(this, mediumRepository)
    override val animationManager: IAnimationManager get() = TODO("not implemented")
    override val selectionEngine: ISelectionEngine get() = TODO("not implemented")
    override val referenceManager: ReferenceManager = ReferenceManager()
    override val paletteSet: PaletteSet get() = TODO("not implemented")

    override val compositor = Compositor()

    override var width: Int by UndoableDelegate(width, undoEngine, "Changed Workspace Width")
    override var height: Int by UndoableDelegate(height, undoEngine, "Changed Workspace Height")


    override fun finishBuilding() {
        undoEngine.reset()
        hasChanged = false
        mediumRepository.locked = false
    }

    override fun fileSaved(newFile: File) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val file: File?
        get() = TODO("not implemented")
    override var hasChanged: Boolean = false


    override val activeData: ArrangedMediumData? get() {
        val node = groupTree.selectedNode
        return when( node) {
            is LayerNode -> arrangeActiveDataForNode(node)
            else -> null
        }
    }
    override fun arrangeActiveDataForNode(node: LayerNode): ArrangedMediumData {
        val layerData = node.layer.activeData
        return layerData.copy(tMediumToWorkspace =  node.tNodeToContext * layerData.tMediumToWorkspace)
    }

    override val activeDrawerBind = Bindable<IImageDrawer>(NillImageDrawer)
    override var activeDrawer: IImageDrawer by activeDrawerBind ; private set

    override fun getDrawerForNode(node: Node) = when( node) {
            is LayerNode -> node.layer.getDrawer(arrangeActiveDataForNode(node))
            else -> NillImageDrawer
    }

    init {
        groupTree.selectedNodeBind.addListener { new, old ->
            activeDrawer = new?.run { getDrawerForNode(this) } ?: NillImageDrawer
        }
    }
}