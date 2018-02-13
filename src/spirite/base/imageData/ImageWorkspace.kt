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
    fun getData( i: Int) : IMedium
    fun getData( handle: MediumHandle) = getData(handle.id)

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

class ImageWorkspace {

    private val mediumData = mutableMapOf<Integer, IMedium>()



}