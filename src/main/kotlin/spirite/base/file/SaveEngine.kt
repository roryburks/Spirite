package spirite.base.file

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import java.io.File
import java.io.RandomAccessFile

interface ISaveEngine {
    fun triggerAutosave( workspace: IImageWorkspace, interval: Int, undoCount: Int)
    fun untriggerAutosave( workspace: IImageWorkspace)
    fun removeAutosavedBackups( workspace: IImageWorkspace)

    val isLocked : Boolean
    fun saveWorkspace( workspace: IImageWorkspace, file: File, track: Boolean = true)

}

class SaveEngine : ISaveEngine {
    override fun triggerAutosave(workspace: IImageWorkspace, interval: Int, undoCount: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun untriggerAutosave(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAutosavedBackups(workspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveWorkspace(workspace: IImageWorkspace, file: File, track: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var lockCount = 0
    override val isLocked: Boolean get() = lockCount > 0

}

class SaveContext(
        val workspace: IImageWorkspace,
        val ra: RandomAccessFile)
{
    val nodeMap = mutableMapOf<Node, Int>()

    fun writeChunk( tag: String, writer : (RandomAccessFile)->Unit) {
        if( tag.length != 4) {
            // Perhaps overkill, but this really should be a hard truth
            MDebug.handleError(ErrorType.FATAL, "Chunk types must be 4-length")
        }

        // [4] : Chunk Tag
        ra.write( tag.toByteArray(charset("UTF-8")))

        val start = ra.filePointer
        // [4] : ChunkLength (placeholder for now
        ra.writeInt(0)

        writer.invoke(ra)

        val end = ra.filePointer
        ra.seek(start)
        if( end - start > Integer.MAX_VALUE)
            MDebug.handleError(ErrorType.OUT_OF_BOUNDS, "Image Data Too Big (>2GB).")
        ra.writeInt((end - start - 4).toInt())
        ra.seek(end)
    }
}

private object Current {

    fun saveWorkspace( file : File, workspace: IImageWorkspace) {
        val overwrite = file.exists()
        val saveFile = if(overwrite) File(file.absolutePath + "~") else file

        if( saveFile.exists())
            saveFile.delete()
        saveFile.createNewFile()

        val ra = RandomAccessFile(file, "rw")
        val context = SaveContext(workspace, ra)

        saveHeader(context)

        ra.close()



    }

    fun saveHeader( context: SaveContext) {
        val ra = context.ra
        val workspace = context.workspace

        // [4] Header
        ra.write( SaveLoadUtil.header)
        // [4] Version
        ra.writeInt(SaveLoadUtil.version)

        // [2] Width, [2] Height
        ra.writeShort(workspace.width)
        ra.writeShort(workspace.height)
    }


    fun saveGroupTree( context: SaveContext)
    {
        context.writeChunk("GRPT", {
            val depth = 0
            var met = 0

            fun buildReferences( node: Node, depth: Int) {
                context.nodeMap.put( node, met++)
                if( node is GroupNode)
                    node.children.forEach { buildReferences(it, depth+1) }
            }
            buildReferences(context.workspace.groupTree.root, depth)

        })

    }
}