package spirite.base.graphics.rendering

import spirite.base.brains.*
import spirite.base.brains.settings.ISettingsManager
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.IThumbnailStore_v2.IThumbnailAccessContract
import spirite.base.graphics.rendering.ThumbnailStore_v2.ReferenceObject
import spirite.base.graphics.rendering.ThumbnailStore_v2.ReferenceObject.*
import spirite.base.graphics.rendering.ThumbnailStore_v2.Thumbnail
import spirite.base.graphics.rendering.sources.GroupNodeSource
import spirite.base.graphics.rendering.sources.LayerSource
import spirite.base.graphics.rendering.sources.MediumSource
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL
import spirite.hybrid.NativeImage
import spirite.pc.graphics.ImageBI
import java.awt.image.BufferedImage
import java.lang.ref.WeakReference

interface IThumbnailStore
{
    fun accessThumbnail( node: Node, workspace: IImageWorkspace) : IImage
}

interface IThumbnailStore_v2<T>
{
    interface IThumbnailAccessContract
    {
        fun release()
    }

    fun contractThumbnail(node: Node, workspace: IImageWorkspace, onBuilt: (T)->Unit) : IThumbnailAccessContract
    fun contractThumbnail(layer: Layer, workspace: IImageWorkspace, onBuilt: (T)->Unit) : IThumbnailAccessContract
    fun contractThumbnail(part :SpritePart, workspace: IImageWorkspace, onBuilt: (T)->Unit ): IThumbnailAccessContract
}


class DerivedThumbnailStore(
        settings: ISettingsManager,
        centralObservatory: ICentralObservatory,
        private val workspaceSet: IWorkspaceSet,
        private val rootThumbnailStore: ThumbnailStore_v2)
    : IThumbnailStore_v2<NativeImage>
{
    private val cache = mutableMapOf<ReferenceObject,NativeImage>()
    private val contracts = mutableMapOf<ReferenceObject,ContractSet>()

    private class ContractSet(val internalContract: IThumbnailAccessContract)
    {
        val externalContracts = mutableListOf<WeakReference<ThumbnailAccessContract>>()
    }

    override fun contractThumbnail(node: Node, workspace: IImageWorkspace, onBuilt: (NativeImage) -> Unit): IThumbnailAccessContract {
        val ref = when(node) {
            is LayerNode -> LayerReference(node.layer, workspace)
            else ->NodeReference(node, workspace)
        }
        cache[ref]?.also(onBuilt)

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(node, workspace,buildLambda(ref,onBuilt)) )

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(layer: Layer, workspace: IImageWorkspace, onBuilt: (NativeImage) -> Unit): IThumbnailAccessContract {
        val ref = LayerReference(layer, workspace)
        cache[ref]?.also(onBuilt)

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(layer, workspace,buildLambda(ref,onBuilt)) )

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(part: SpritePart, workspace: IImageWorkspace, onBuilt: (NativeImage) -> Unit): IThumbnailAccessContract {
        val ref = SpritePartReference(part, workspace)
        cache[ref]?.also(onBuilt)

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(part, workspace,buildLambda(ref,onBuilt)) )

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    private fun buildLambda( ref: ReferenceObject, onBuilt: (NativeImage) -> Unit) : (IImage)->Unit
    {
        return {img ->
            val contractSet = contracts[ref]
            if( contractSet != null)
            {
                contractSet.externalContracts.removeIf { it.get() == null }
                if( !contractSet.externalContracts.any()) {
                    contracts.remove(ref)
                    contractSet.internalContract.release()
                }
                else {
                    val native = Hybrid.imageConverter.convert<NativeImage>(img)
                    cache[ref] = native
                    contractSet.externalContracts.forEach { onBuilt.invoke(native)}
                }
            }
        }
    }

    private inner class ThumbnailAccessContract
    internal constructor(
            internal val ref: ReferenceObject,
            val onBuilt: (NativeImage) -> Unit )
        : IThumbnailAccessContract
    {
        override fun release() {
            val set = contracts[ref]
            if( set != null) {
                set.externalContracts.removeIf {
                    val contract = it.get()
                    contract == null || contract == this
                }
                if( !set.externalContracts.any()) {
                    contracts.remove(ref)
                    set.internalContract.release()
                }
            }
        }
    }
}

class ThumbnailStore_v2(
        settings: ISettingsManager,
        centralObservatory: ICentralObservatory,
        private val workspaceSet: IWorkspaceSet) : IThumbnailStore_v2<IImage>
{
    internal sealed class ReferenceObject {
        abstract val workspace : IImageWorkspace
        data class NodeReference(val node: Node, override val workspace: IImageWorkspace) : ReferenceObject()
        data class LayerReference(val layer: Layer, override val workspace: IImageWorkspace) : ReferenceObject()
        data class SpritePartReference(val part: SpritePart, override val workspace: IImageWorkspace) : ReferenceObject()
    }

    private val thumbnailCache = mutableMapOf<ReferenceObject,Thumbnail>()
    private val cacheCheckFrequency = settings.thumbnailCacheCheckFrequency
    private val lifespan = settings.thumbnailLifespan

    override fun contractThumbnail(node: Node, workspace: IImageWorkspace, onBuilt: (IImage) -> Unit): IThumbnailAccessContract {
        val ref = when(node) {
            is LayerNode -> LayerReference(node.layer, workspace)
            else ->NodeReference(node, workspace)
        }
        tryAccessThumbnail(ref)?.also { onBuilt.invoke(it.image) }


        val contract = ThumbnailAccessContract(ref, onBuilt)
        contracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(layer: Layer, workspace: IImageWorkspace, onBuilt: (IImage) -> Unit): IThumbnailAccessContract {
        val ref = LayerReference(layer, workspace)
        tryAccessThumbnail(ref)?.also { onBuilt.invoke(it.image) }

        val contract = ThumbnailAccessContract(ref, onBuilt)
        contracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(part: SpritePart, workspace: IImageWorkspace, onBuilt: (IImage) -> Unit): IThumbnailAccessContract {
        val ref = SpritePartReference(part, workspace)
        tryAccessThumbnail(ref)?.also { onBuilt.invoke(it.image) }

        val contract = ThumbnailAccessContract(ref, onBuilt)
        contracts.add(WeakReference(contract))
        return contract
    }

    private fun tryAccessThumbnail(ref: ReferenceObject) : Thumbnail?
    {
        val existing = thumbnailCache[ref]
        if (existing != null) {
            val now = Hybrid.timing.currentMilli
            if( !existing.changed || (now - existing.made) < lifespan)
            {
                return existing
            }
        }
        return null
    }

    private val contracts = mutableListOf<WeakReference<ThumbnailAccessContract>>()

    private inner class ThumbnailAccessContract
    internal constructor(
            internal val ref: ReferenceObject,
            val onBuilt: (IImage) -> Unit )
        : IThumbnailAccessContract
    {
        override fun release() {
            contracts.removeIf {
                val contract = it.get()
                contract == null || contract == this
            }
        }
    }

    private class Thumbnail(val image: RawImage){
        var changed = false
        var made = Hybrid.timing.currentMilli
    }

    // region Work Cycle
    private fun removeUnused()
    {
        val existingReferences = contracts.mapNotNull { it.get()?.ref }.toHashSet()
        thumbnailCache.entries.removeIf { entry ->
            when {
                !existingReferences.contains(entry.key) -> {
                    entry.value.image.flush()
                    true
                }
                else -> false
            }
        }
    }

    private fun cycleContracts()
    {
        // Go through each contract
        //  -removing the weak references that disappeared
        //  -creating a new thumbnail and triggering the onBuilt for contracts that have either aged out or don't exist
        val now = Hybrid.timing.currentMilli
        contracts.removeIf {
            val contract = it.get()
            when( contract) {
                null -> true
                else -> {
                    val thumbnail = thumbnailCache[contract.ref]
                    if(thumbnail == null || (thumbnail.changed && thumbnail.made - now > lifespan)) {
                        val updatedThumbnail = createOrUpdateThumbnail(contract.ref)
                        contract.onBuilt.invoke(updatedThumbnail.image)
                    }
                    false
                }
            }
        }
    }

    private fun createOrUpdateThumbnail(ref: ReferenceObject) : Thumbnail
    {
        val source = when( ref) {
            is LayerReference -> LayerSource(ref.layer,ref.workspace)
            is SpritePartReference -> MediumSource(ref.part.handle,ref.workspace)
            is NodeReference -> when(ref.node) {
                is GroupNode -> GroupNodeSource(ref.node, ref.workspace)
                is LayerNode -> {
                    MDebug.handleWarning(STRUCTURAL, "Shouldn't be able to have a NodeReference that is a LayerNode (it should get short-circuited into a LayerReference")
                    LayerSource(ref.node.layer, ref.workspace)
                }
                else -> throw Exception("Unrenderable node")
            }
        }

        val existing = thumbnailCache[ref]

        val thumbnail = when( existing) {
            null -> Thumbnail(Hybrid.imageCreator.createImage(32,32)).also { thumbnailCache[ref] = it }
            else -> existing.also { it.made = Hybrid.timing.currentMilli }
        }

        val gc= thumbnail.image.graphics
        gc.clear()
        source.render(RenderSettings(32, 32, false), gc)

        return thumbnail
    }

    init {
        Hybrid.timing.createTimer(1000, true) {
            println("tick")
            removeUnused()
            cycleContracts()
        }
    }
    // endregion

    init {
        centralObservatory.trackingImageObserver.addObserver(object : ImageObserver {
            override fun imageChanged(evt: ImageChangeEvent) {
                val workspace = workspaceSet.currentWorkspace ?: return
                val mediums = evt.handlesChanged.toHashSet()
                val nodes = evt.nodesChanged.toHashSet()

                thumbnailCache.forEach { ref, thumbnail ->
                    if( workspace == ref.workspace && !thumbnail.changed) {
                        when( ref) {
                            is SpritePartReference -> if( mediums.contains(ref.part.handle)) thumbnail.changed = true
                            is LayerReference -> if( ref.layer.imageDependencies.any { mediums.contains(it) }) thumbnail.changed = true
                            is NodeReference ->
                                if( ref.node.descendants.any{nodes.contains(it)} || ref.node.imageDependencies.any { mediums.contains(it) })
                                    thumbnail.changed = true
                        }
                    }
                }
            }
        })
    }
}

interface IThumbnailStoreBi
{
    fun accessThumbnail( node: Node, workspace: IImageWorkspace) : ImageBI
}
class ThumbnailStoreBi(settings: ISettingsManager, centralObservatory: ICentralObservatory, private val originalStore: IThumbnailStore): IThumbnailStoreBi
{
    private val useOnlyBi = false
    private val onlyBi = ImageBI(BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR))

    override fun accessThumbnail(node: Node, workspace: IImageWorkspace) : ImageBI {
        if( useOnlyBi) return onlyBi

        val key = Pair(workspace, node)

        val now = Hybrid.timing.currentMilli

        val existing = store[key]
        if (existing != null) {
            if( existing.changed && now - existing.made < lifespan)
            {
                existing.image.flush()
                store.remove(key)
            }
            else return existing.image
        }

        val orig = originalStore.accessThumbnail(node, workspace)
        val bi = Hybrid.imageConverter.convert<ImageBI>(orig)

        store[key] = ThumbnailBi(bi)

        return bi
    }

    private val store = mutableMapOf<Pair<IImageWorkspace,Node>,ThumbnailBi>()
    private var lastCacheClear = 0L

    private val cacheCheckFrequency = settings.thumbnailCacheCheckFrequency
    private val lifespan = settings.thumbnailLifespan

    private class ThumbnailBi(val image: ImageBI){
        var changed = false
        val made = Hybrid.timing.currentMilli
    }

    private val _imageObserver = object : ImageObserver {
        override fun imageChanged(evt: ImageChangeEvent) {
            val mediums = evt.handlesChanged.toHashSet()
            val nodes = evt.nodesChanged.toHashSet()

            for (entry in store.filter { !it.value.changed }) {
                if( entry.key.first == evt.workspace) {
                    if( entry.key.second.imageDependencies.any { mediums.contains(it) } ||
                            entry.key.second.getAllNodesSuchThat({true}, {true}).any { nodes.contains(it) })
                    {
                        entry.value.changed = true
                    }
                }
            }

            val now = Hybrid.timing.currentMilli
            if( now - lastCacheClear > cacheCheckFrequency ) {
                store.entries.removeIf { it.value.changed && (now - it.value.made > lifespan).apply { it.value.image.flush()} }
                lastCacheClear = now
            }
        }
    }.also { centralObservatory.trackingImageObserver.addObserver(it)  }

}

class ThumbnailStore(settings: ISettingsManager, centralObservatory: ICentralObservatory) : IThumbnailStore {


    override fun accessThumbnail(node: Node, workspace: IImageWorkspace) : IImage {
        val key = Pair(workspace, node)

        val now = Hybrid.timing.currentMilli

        val existing = store[key]
        if (existing != null) {
            if( existing.changed && now - existing.made < lifespan)
            {
                existing.image.flush()
                store.remove(key)
            }
            else {
                return existing.image
            }
        }

        val source = when( node) {
            is LayerNode -> LayerSource(node.layer, workspace)
            is GroupNode -> GroupNodeSource(node, workspace)
            else -> throw Exception("Unrenderable node")
        }

        val thumbnail = Hybrid.imageCreator.createImage(32, 32)
        source.render(RenderSettings(32, 32, false), thumbnail.graphics)

        store[key] = Thumbnail(thumbnail)

        return thumbnail
    }

    private val store = mutableMapOf<Pair<IImageWorkspace,Node>,Thumbnail>()
    private var lastCacheClear = 0L

    private val cacheCheckFrequency = settings.thumbnailCacheCheckFrequency
    private val lifespan = settings.thumbnailLifespan

    private class Thumbnail(val image: RawImage){
        var changed = false
        val made = Hybrid.timing.currentMilli
    }

    private val _imageObserver = object : ImageObserver {
        override fun imageChanged(evt: ImageChangeEvent) {
            val mediums = evt.handlesChanged.toHashSet()
            val nodes = evt.nodesChanged.toHashSet()

            for (entry in store.filter { !it.value.changed }) {
                if( entry.key.first == evt.workspace) {
                    if( entry.key.second.imageDependencies.any { mediums.contains(it) } ||
                        entry.key.second.getAllNodesSuchThat({true}, {true}).any { nodes.contains(it) })
                    {
                        entry.value.changed = true
                    }
                }
            }

            val now = Hybrid.timing.currentMilli
            if( now - lastCacheClear > cacheCheckFrequency ) {
                store.entries.removeIf { it.value.changed && (now - it.value.made > lifespan).apply { it.value.image.flush()} }
                lastCacheClear = now
            }
        }
    }.also { centralObservatory.trackingImageObserver.addObserver(it)  }
}