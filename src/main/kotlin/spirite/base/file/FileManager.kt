package spirite.base.file

import rb.file.BufferedReadStream
import rb.global.ConsoleLogger
import rb.glow.Colors
import rb.glow.IImageConverter
import rb.glow.img.IImage
import rb.glow.using
import rbJvm.file.JvmRandomAccessFileBinaryReadStream
import sgui.core.systems.IImageCreator
import spirite.base.brains.IMasterControl
import spirite.base.file.sif.v1.load.BadSifFileException
import spirite.base.file.sif.v1.load.LoadEngine
import spirite.base.file.sif.v1.save.SaveEngine
import spirite.base.file.sif.v2.JvmSpiriteSaveLoad
import spirite.base.file.sif.v2.import.SifWorkspaceImporter
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.FlatMedium
import spirite.core.file.contracts.SifFile
import spirite.core.file.load.SifFileReader
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.DiSet_Hybrid
import spirite.core.hybrid.IDebug
import spirite.core.hybrid.IDebug.ErrorType.FILE
import spirite.sguiHybrid.Hybrid
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile


fun IMasterControl.workspaceFromImage(img: IImage) {
    val workspace = createWorkspace(img.width,img.height)
    val medium = FlatMedium(DiSet_Hybrid.imageConverter.convertToInternal(img), workspace.mediumRepository)
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

class FileManager(
    val master: IMasterControl,
    private val _debug : IDebug = DebugProvider.debug,
    private val _imageCreator : IImageCreator = DiSet_Hybrid.imageCreator)
    : IFileManager
{
    companion object {
        var v2Load: Boolean = true
        var v2Save: Boolean = true
    }

    private val _sifImporter = SifWorkspaceImporter(Hybrid.imageIO,ConsoleLogger)

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
    private val locks = mutableListOf<FileLock>()
    override fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean) {
        workspace.paletteMediumMap.clearUnused()

        val lock = FileLock().apply { locks.add(this) }

        if( v2Save)
            JvmSpiriteSaveLoad.write(file, workspace)
        else
            SaveEngine.saveWorkspace(file, workspace)
        workspace.fileSaved(file)
        locks.remove(lock)
    }

    override fun exportToImage(workspace: IImageWorkspace, file: File) {
        val ext = file.name.substring( file.name.lastIndexOf('.')+1).toLowerCase()

        when( ext) {
            "jpg", "jpeg" -> {
                // Remove Alpha Layer of JPG so that it works correctly with encoding
                val wsImage = master.renderEngine.renderWorkspace(workspace)
                using(_imageCreator.createImage(wsImage.width, wsImage.height)) { img ->
                    val gc = img.graphics
                    gc.clear(Colors.WHITE)
                    gc.renderImage(wsImage, 0.0, 0.0)

                    Hybrid.imageIO.saveImage(wsImage, file) // intentionally changed from img.  seems SLIGHTLY less completely broken
                }
            }
            else -> Hybrid.imageIO.saveImage( master.renderEngine.renderWorkspace(workspace), file)
        }
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
            // Try to load it as an SIF (either if the extension isn't recognized or if ImageIO failed)
            val workspace = if( v2Load) {
                val ra = RandomAccessFile(file, "r")
                val read = BufferedReadStream(JvmRandomAccessFileBinaryReadStream(ra))
                val sif : SifFile
                try {
                    sif = SifFileReader.read(read)
                } finally {
                    ra.close()
                }
                _sifImporter.import(sif, master)
            } else
                LoadEngine.loadWorkspace(file, master)
            workspace.fileSaved(file)
            master.workspaceSet.addWorkspace(workspace, true)
            // TODO: Trigger autosave
            return
        } catch (e: BadSifFileException){
            _debug.handleError(FILE, e.message ?: "Failed to Load SIF", e)
            throw e
        }
        if( !attempted) {
            try {
                val img = Hybrid.imageIO.loadImage(file.readBytes())
                master.workspaceFromImage(img)
            }catch (e: Exception){}
        }
    }
}