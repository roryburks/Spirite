package spirite.base.imageData

import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.util.linear.MutableTransform
import java.io.File

class BuildingMediumData {
    val handle: MediumHandle
    var trans: MutableTransform
    var color: Int = 0

    constructor( handle: MediumHandle) {
        this.handle = handle
        trans = MutableTransform.IdentityMatrix()
    }
    constructor( handle: MediumHandle, ox: Float, oy: Float ) {
        this.handle = handle
        trans = MutableTransform.TranslationMatrix(ox, oy)
    }

    fun doOnBuildData( doer : (BuiltMediumData) -> Unit) {
        //handle.context.doOnBuiltData(this, doer)
    }

}

interface IImageWorkspace {
    var width: Int
    var height: Int

    // File-Related
    fun finishBuilding()
    fun fileSaved( newFile: File)
    val file: File?
    val filename get() = file?.name ?: "Untitled Image"
    val hasChanged: Boolean

    val groupTree: GroupTree


	// Sub-Components
    val undoEngine : IUndoEngine
    val animationManager : IAnimationManager
    val selectionEngine : ISelectionEngine
    val referenceManager : ReferenceManager
    val paletteSet : PaletteSet
    val mediumRepository : IMediumRepository
//	public StagingManager getStageManager() {return stagingManager;}

    // Super-Components
    val renderEngine : IRenderEngine
    val settingsManager: ISettingsManager
    val paletteManager : IPaletteManager

//    val rootNode: GroupTree.GroupNode
    val images: List<MediumHandle>

    val activeDrawer: IImageDrawer
//    fun getDrawerFromNode( node: Node) : IImageDrawer
    fun getDrawerFromBMD( bmd: BuildingMediumData) : IImageDrawer
    fun getDrawerFromHandle( handle: MediumHandle) : IImageDrawer

    fun buildActiveData() : BuildingMediumData


}

class ImageWorkspace(

) : IImageWorkspace{

    override val undoEngine = UndoEngine()
    override val mediumRepository = MediumRepository( undoEngine, this)

    override var width: Int
        get() = TODO("not implemented")
        set(value) {}
    override var height: Int
        get() = TODO("not implemented")
        set(value) {}

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
    override val groupTree: GroupTree
        get() = TODO("not implemented")
    override val animationManager: IAnimationManager
        get() = TODO("not implemented")
    override val selectionEngine: ISelectionEngine
        get() = TODO("not implemented")
    override val referenceManager: ReferenceManager
        get() = TODO("not implemented")
    override val paletteSet: PaletteSet
        get() = TODO("not implemented")
    override val renderEngine: IRenderEngine
        get() = TODO("not implemented")
    override val settingsManager: ISettingsManager
        get() = TODO("not implemented")
    override val paletteManager: IPaletteManager
        get() = TODO("not implemented")
    override val images: List<MediumHandle>
        get() = TODO("not implemented")
    override val activeDrawer: IImageDrawer
        get() = TODO("not implemented")

    override fun getDrawerFromBMD(bmd: BuildingMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDrawerFromHandle(handle: MediumHandle): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildActiveData(): BuildingMediumData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}