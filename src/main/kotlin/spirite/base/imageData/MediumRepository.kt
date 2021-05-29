package spirite.base.imageData

import spirite.sguiHybrid.Hybrid
import spirite.sguiHybrid.MDebug
import spirite.sguiHybrid.MDebug.ErrorType.STRUCTURAL
import spirite.base.imageData.mediums.IMedium

/**
 *  The MediumRepository is responsible for storing the direct medium data.
 *
 *  TODO: Add with(Lock) to mediums (when condensing data, on changeMedium, and on
 */

/** Read-only Access to the Medium Repository */
interface IMediumRepository {
    fun getData( i: Int) : IMedium?

    val dataList : List<Int>
    fun <T> floatData( i: Int, condenser: (IMedium)->T) : IFloatingMedium<T>?
}

interface IFloatingMedium<T> {
    fun flush()
    val condensed : T
    val id: Int
}

/** Read-Write Access to the Medium Repository */
interface MMediumRepository : IMediumRepository {
    /** Adds a medium to the repository and returns a MediumHandle that references it. */
    fun addMedium( medium: IMedium) : MediumHandle

    /** Imports all of the IMediums into the repository and returns a map from the
     * medium's index in the old map to its index in the repository
     */
    fun importMap( map: Map<Int,IMedium>) : Map<Int,Int>


    fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium)
    fun clearUnusedCache(externalDataUsed : Set<MediumHandle>)
    fun getUnused(externalDataUsed : Set<MediumHandle>) : List<MediumHandle>
    fun changeMedium( i: Int, runner: (IMedium)->Unit)
}


private class MediumRepoEntry(val medium: IMedium) {
    var floating = false
}

class MediumRepository(private val imageWorkspace: IImageWorkspace)
    : MMediumRepository
{
    private val mediumData = mutableMapOf<Int,MediumRepoEntry>()
    private var workingId = 0

    /** Locks the cache from being cleared. */
    var locked : Boolean = true


    // region IMedium
    override fun getData(i: Int) = mediumData[i]?.medium

    override val dataList: List<Int> get() = mediumData.map { it.key }

    val floatingData = mutableListOf<FloatingMedium<*>>()
    override fun <T> floatData( i: Int, condenser: (IMedium)->T) : IFloatingMedium<T>? {
        return if( mediumData[i] == null) null
            else FloatingMedium(i, condenser)
    }

    inner class FloatingMedium<T>
    internal constructor(
            override val id: Int,
            val condenser: (IMedium)->T)
        : IFloatingMedium<T>
    {
        init {floatingData.add(this)}
        override fun flush() {floatingData.remove(this)}
        override val condensed: T by lazy { condenser.invoke( mediumData[id]!!.medium) }
    }
    // endregion


    // region MMedium
    override fun clearUnusedCache( externalDataUsed : Set<MediumHandle>) {
        if( locked) return

        val externalImageIds = externalDataUsed.map { it.id }

        val layerImages =  imageWorkspace.groupTree.root.getLayerNodes()
                .flatMap { it.layer.imageDependencies }
        val layerImageIds = layerImages.map { it.id }.distinct()

        val unused = mediumData.keys
                .filter {!externalImageIds.contains( it) && !layerImageIds.contains(it)}

        // Make sure all used entries are tracked
        if( layerImages.any { it.workspace != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found when cleaning ImageWorkspace")
        if( externalDataUsed.any { it.workspace != imageWorkspace || mediumData[it.id] == null })
            MDebug.handleError(STRUCTURAL, "Untracked Image Data found from UndoEngine")

        // Remove Unused Entries
        unused.forEach {unusedIndex ->
            mediumData[unusedIndex]?.also { mediumDatum ->
                // Invoke so that they get copied
                floatingData.filter { it.id == unusedIndex }.forEach { it.condensed }
                mediumDatum.medium.flush()
                mediumData.remove(unusedIndex)
            }
        }
    }

    override fun getUnused(externalDataUsed: Set<MediumHandle>): List<MediumHandle> {
        val externalImageIds = externalDataUsed.map { it.id }

        val layerImages =  imageWorkspace.groupTree.root.getLayerNodes()
                .flatMap { it.layer.imageDependencies }
        val layerImageIds = layerImages.map { it.id }.distinct()

        val unused = mediumData.keys
                .filter {!externalImageIds.contains( it) && !layerImageIds.contains(it)}
        return  unused.map { MediumHandle(imageWorkspace, it) }
    }

    override fun addMedium(medium: IMedium) : MediumHandle{
        mediumData[workingId] = MediumRepoEntry(medium)
        return MediumHandle(imageWorkspace, workingId++)
    }

    override fun importMap(map: Map<Int, IMedium>): Map<Int, Int> {
        val indexMap = mutableMapOf<Int,Int>()

        map.forEach { key, value ->
            mediumData[workingId] = MediumRepoEntry(value)
            indexMap.put(key,workingId++)
        }

        return indexMap
    }

    override fun replaceMediumDirect(handle: MediumHandle, newMedium: IMedium) {
        val oldMedium = mediumData[handle.id]
        mediumData.put( handle.id, MediumRepoEntry(newMedium))
        oldMedium?.medium?.flush()
    }


    override fun changeMedium( i: Int, runner: (IMedium)->Unit) {
        val mediumDatum = mediumData[i] ?: return
        Hybrid.LockFrom(mediumDatum).withLock {
            // Invoke so that they get copied
            floatingData.filter { it.id == i }.forEach { it.condensed }
            runner.invoke(mediumDatum.medium)
        }
    }

    // endregion
}