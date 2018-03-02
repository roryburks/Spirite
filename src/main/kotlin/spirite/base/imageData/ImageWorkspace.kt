package spirite.base.imageData

import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteSet
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.Compositor
import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.imageData.selection.ISelectionEngine
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.UndoEngine
import spirite.base.pen.stroke.IStrokeDrawer
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

    val groupTree: PrimaryGroupTree


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
    val strokeProvider : IStrokeDrawerProvider

    val activeDrawer: IImageDrawer
//    fun getDrawerFromNode( node: Node) : IImageDrawer
    fun getDrawerFromBMD( bmd: ArrangedMediumData) : IImageDrawer
    fun getDrawerFromHandle( handle: MediumHandle) : IImageDrawer

    fun buildActiveData() : ArrangedMediumData

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



    // Note: while this technically leaks access, everything which should have limited access (i.e. virtually everything
    //  outside of unit tests) should only have an IImageWorkspace, not an ImageWorkspace.
    override val mediumRepository = MediumRepository( this)
    override val undoEngine = UndoEngine(this, mediumRepository)
    override val imageObservatory: IImageObservatory = ImageObservatory()
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
    override val groupTree = PrimaryGroupTree(this, mediumRepository)
    override val animationManager: IAnimationManager
        get() = TODO("not implemented")
    override val selectionEngine: ISelectionEngine
        get() = TODO("not implemented")
    override val referenceManager: ReferenceManager
        get() = TODO("not implemented")
    override val paletteSet: PaletteSet
        get() = TODO("not implemented")
    override val activeDrawer: IImageDrawer
        get() = TODO("not implemented")

    override fun getDrawerFromBMD(bmd: ArrangedMediumData): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDrawerFromHandle(handle: MediumHandle): IImageDrawer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildActiveData(): ArrangedMediumData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}