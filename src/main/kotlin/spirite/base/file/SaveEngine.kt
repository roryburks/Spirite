package spirite.base.file

import spirite.base.imageData.ImageWorkspace
import java.io.File

interface ISaveEngine {
    fun triggerAutosave( workspace: ImageWorkspace, interval: Int, undoCount: Int)
    fun untriggerAutosave( workspace: ImageWorkspace)
    fun removeAutosavedBackups( workspace: ImageWorkspace)

    fun saveWorkspace( workspace: ImageWorkspace, file: File, track: Boolean = true)

}

class SaveEngine : ISaveEngine {
    override fun triggerAutosave(workspace: ImageWorkspace, interval: Int, undoCount: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun untriggerAutosave(workspace: ImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAutosavedBackups(workspace: ImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveWorkspace(workspace: ImageWorkspace, file: File, track: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}