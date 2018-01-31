package spirite.base.imageData

import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.drawer.IImageDrawer
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

    fun finishBuilding()
    fun fileSaved( newFile: File)
    val file: File?
    val filename get() = file?.name ?: "Untitled Image"

    var width: Int
    var height: Int

    val hasChanged: Boolean

	// Sub-Components
    val undoEngine : IUndoEngine
//	public AnimationManager getAnimationManager() { return animationManager; }
//	public SelectionEngine getSelectionEngine() { return selectionEngine; }
//	public ReferenceManager getReferenceManager() { return referenceManager; }
//	public StagingManager getStageManager() {return stagingManager;}
//	public PaletteSet getPaletteSet() {return paletteSet;}
//
//	// Super-Components (components rooted in MasterControl, simply being passed on)
//	public RenderEngine getRenderEngine() { return renderEngine; }
//	public SettingsManager getSettingsManager() {return master.getSettingsManager();}
//	public PaletteManager getPaletteManager() {return paletteManager;}	// Might be removed in the future

//    val rootNode: GroupTree.GroupNode
    val images: List<MediumHandle>

    val activeDrawer: IImageDrawer
//    fun getDrawerFromNode( node: Node) : IImageDrawer
    fun getDrawerFromBMD( bmd: BuildingMediumData) : IImageDrawer
    fun getDrawerFromHandle( handle: MediumHandle) : IImageDrawer

    fun buildActiveData() : BuildingMediumData
}

class ImageWorkspace {

}