package spirite.base.file

import spirite.base.imageData.IImageWorkspace
import java.io.File

interface IFileManager {
    fun triggerAutosave( workspace: IImageWorkspace, interval: Int, undoCount: Int)
    fun untriggerAutosave( workspace: IImageWorkspace)
    fun removeAutosavedBackups( workspace: IImageWorkspace)

    val isLocked : Boolean

    fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean = true)
}

class FileManager  : IFileManager{
    override fun triggerAutosave(workspace: IImageWorkspace, interval: Int, undoCount: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun untriggerAutosave(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAutosavedBackups(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val isLocked: Boolean
        get() = TODO("not implemented")

    class FileLock
    val locks = mutableListOf<FileLock>()
    override fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean) {
        val lock = FileLock().apply { locks.add(this) }
        SaveEngine.saveWorkspace(file, workspace)
        locks.remove(lock)
    }

}