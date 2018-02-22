package spirite.base.imageData

import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.NilMedium
import spirite.base.imageData.undo.IUndoEngine
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

/**
 *  The MediumRepository is responsible for storing the direct medium data.
 */

/** Read-only Access to the Medium Repository */
interface IMediumRepository {
    fun getData( i: Int) : IMedium?
}

/** Read-Write Access to the Medium Repository */
interface MMediumRepository : IMediumRepository {
    /** Adds a medium to the repository and returns a MediumHandle that references it. */
    fun addMedium( medium: IMedium) : MediumHandle


    fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium)
    fun clearUnusedCache(externalDataUsed : Set<MediumHandle>)
}

class MediumRepository(
        private val imageWorkspace: IImageWorkspace
) : MMediumRepository{
    val mediumData = mutableMapOf<Int,IMedium>()
    var workingId = 0

    /** Locks the cache from being cleared. */
    var locked : Boolean = true

    // region IMedium
    override fun getData(i: Int) = mediumData[i]
    // endregion


    // region MMedium

    override fun clearUnusedCache( externalDataUsed : Set<MediumHandle>) {
        if( locked) return

        val externalImageIds = externalDataUsed.map { it.id }

        val layerImages =  imageWorkspace.groupTree.root.getLayerNodes()
                .map { it.layer.imageDependencies }
                .reduce { acc, list ->  acc.union(list).toList()}
        val layerImageIds = layerImages.map { it.id }.distinct()

        val unused = mediumData.keys
                .filter {externalImageIds.contains( it) || layerImageIds.contains(it)}

        // Make sure all used entries are tracked
        if( layerImages.any { it.workspace != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found when cleaning ImageWorkspace")
        if( externalDataUsed.any { it.workspace != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found from UndoEngine")

        // Remove Unused Entries
        unused.forEach {
            mediumData[it]?.flush()
            mediumData.remove(it)
        }
    }

    override fun addMedium(medium: IMedium) : MediumHandle{
        mediumData[workingId] = medium
        return MediumHandle(imageWorkspace, workingId++)
    }

    override fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium) {
        val oldMedium = mediumData[handle.id]
        mediumData.put( handle.id, newMedium)
        oldMedium?.flush()
    }

    // endregion
}