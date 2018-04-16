package spirite.base.file

import spirite.base.brains.IMasterControl
import spirite.base.brains.MasterControl
import spirite.base.graphics.IImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.util.Colors
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL
import java.awt.Color
import java.io.File
import java.io.IOException


fun IMasterControl.workspaceFromImage(img: IImage) {
    val workspace = createWorkspace(img.width,img.height)
    val medium = FlatMedium(Hybrid.imageConverter.convertToInternal(img), workspace.mediumRepository)
    val layer = SimpleLayer( workspace.mediumRepository.addMedium(medium))
    workspace.groupTree.importLayer(null, "base", layer)
    workspace.finishBuilding()
    workspaceSet.addWorkspace(workspace)
}

interface IFileManager {
    fun triggerAutosave( workspace: IImageWorkspace, interval: Int, undoCount: Int)
    fun untriggerAutosave( workspace: IImageWorkspace)
    fun removeAutosavedBackups( workspace: IImageWorkspace)

    val isLocked : Boolean

    fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean = true)
    fun exportToImage( workspace: IImageWorkspace, file: File)

    fun openFile( file: File)
}

class FileManager( val master: IMasterControl)  : IFileManager{

    override fun triggerAutosave(workspace: IImageWorkspace, interval: Int, undoCount: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun untriggerAutosave(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAutosavedBackups(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val isLocked: Boolean get() = locks.isNotEmpty()

    class FileLock
    val locks = mutableListOf<FileLock>()
    override fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean) {
        val lock = FileLock().apply { locks.add(this) }
        SaveEngine.saveWorkspace(file, workspace)
        workspace.fileSaved(file)
        locks.remove(lock)
    }

    override fun exportToImage(workspace: IImageWorkspace, file: File) {
        val ext = file.name.substring( file.name.lastIndexOf('.')+1).toLowerCase()

        val imageToSave : IImage
        if( ext == "jpg" || ext == "jpeg") {
            // Remove Alpha Layer of JPG so that it works correctly with encoding
            val wsImage = master.renderEngine.renderWorkspace(workspace)
            val img2 = Hybrid.imageCreator.createImage(wsImage.width, wsImage.height)
            val gc = img2.graphics
            gc.clear( Colors.WHITE)
            gc.renderImage(wsImage, 0, 0)

            imageToSave = img2
            wsImage.flush()
        }
        else imageToSave = master.renderEngine.renderWorkspace(workspace)

        try {
            Hybrid.imageIO.saveImage( imageToSave, file)
        }catch (e: Exception) {
            MDebug.handleWarning(STRUCTURAL, "Failed to Export file: ${e.message}", e);
        }
        imageToSave.flush()
    }

    override fun openFile(file: File) {
        val ext = file.name.substring( file.name.lastIndexOf('.')+1).toLowerCase()

        var attempted = false
        if( ext == "png" || ext == "bmp" || ext == "jpg" || ext == "jpeg") {
            // First try to load the file as if it's a standard file format
            try {
                val img = Hybrid.imageIO.loadImage(file.readBytes())
                master.workspaceFromImage(img)
                return
            } catch( e: IOException) {
                attempted = true
            }
        }
        try {
            // Try to load it as an SIF (either if the extention isn't recognized or if ImageIO failed)
            val workspace = LoadEngine.loadWorkspace(file, master)
            master.workspaceSet.addWorkspace(workspace, true)
            // TODO: Trigger autosave
            return
        } catch (e: BadSifFileException){}
        if( !attempted) {
            try {
                val img = Hybrid.imageIO.loadImage(file.readBytes())
                master.workspaceFromImage(img)
            }catch (e: Exception){}
        }
    }
}