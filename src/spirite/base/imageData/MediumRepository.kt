package spirite.base.imageData

import spirite.base.imageData.mediums.IMedium
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

interface IMediumRepository {
    fun getData( i: Int) : IMedium
    fun clearUnusedCache()
}

class MediumRepository(
        private val undoEngine: IUndoEngine,
        private val imageWorkspace: IImageWorkspace
) : IMediumRepository{
    val mediumData = mutableMapOf<Int,IMedium>()

    override fun getData(i: Int): IMedium {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearUnusedCache() {
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
}