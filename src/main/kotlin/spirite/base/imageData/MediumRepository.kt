package spirite.base.imageData

import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.undo.IUndoEngine
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

/**
 *  The MediumRepository is responsible for storing the direct medium data
 */
interface IMediumRepository {
    fun getData( i: Int) : IMedium
    fun clearUnusedCache()

    /** WARNING: This method directly replaces the underlying data and us NOT UNDOABLE.  In general should only be called
     * by the UndoEngine. */
    fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium)
}

class MediumRepository(
        private val undoEngine: IUndoEngine,
        private val imageWorkspace: IImageWorkspace
) : IMediumRepository{
    val mediumData = mutableMapOf<Int,IMedium>()

    /** Locks the cache from being cleared. */
    var locked : Boolean = true

    override fun getData(i: Int) = mediumData[i]!!

    override fun clearUnusedCache() {
        if( locked) return

        val undoImages = undoEngine.dataUsed
        val undoImageIds = undoImages.map { it.id }

        val layerImages =  imageWorkspace.groupTree.root.getLayerNodes()
                .map { it.layer.imageDependencies }
                .reduce { acc, list ->  acc.union(list).toList()}
        val layerImageIds = layerImages.map { it.id }.distinct()

        val unused = mediumData.keys
                .filter {undoImageIds.contains( it) || layerImageIds.contains(it)}

        // Make sure all used entries are tracked
        if( layerImages.any { it.context != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found when cleaning ImageWorkspace")
        if( undoImages.any { it.context != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found from UndoEngine")

        // Remove Unused Entries
        unused.forEach {
            mediumData[it]?.flush()
            mediumData.remove(it)
        }
    }

    override fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium) {
        val oldMedium = mediumData[handle.id]
        mediumData.put( handle.id, newMedium)
        oldMedium?.flush()
    }
}