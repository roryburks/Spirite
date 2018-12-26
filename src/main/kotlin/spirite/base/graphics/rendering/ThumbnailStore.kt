package spirite.base.graphics.rendering

import rb.owl.observer
import spirite.base.brains.ICentralObservatory
import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.settings.ISettingsManager
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.graphics.rendering.ThumbnailStore.ReferenceObject
import spirite.base.graphics.rendering.ThumbnailStore.ReferenceObject.*
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
import java.lang.ref.WeakReference

interface IThumbnailStore<T>
{
    interface IThumbnailAccessContract
    {
        fun release()
    }

    fun contractThumbnail(node: Node, workspace: IImageWorkspace, onBuilt: (T)->Unit) : IThumbnailAccessContract
    fun contractThumbnail(layer: Layer, workspace: IImageWorkspace, onBuilt: (T)->Unit) : IThumbnailAccessContract
    fun contractThumbnail(part :SpritePart, workspace: IImageWorkspace, onBuilt: (T)->Unit ): IThumbnailAccessContract
}


class DerivedNativeThumbnailStore(private val rootThumbnailStore: ThumbnailStore)
    : IThumbnailStore<NativeImage>
{
    // Note: we kind of just drop off BufferedImages without giving any care to resource recovery, assuming
    //  (a) they aren't that big anyway (32x32)
    //  (b) BufferedImages handle themselves properly

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

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(node, workspace,buildLambda(ref,onBuilt)) ).also { contracts[ref] = it }

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(layer: Layer, workspace: IImageWorkspace, onBuilt: (NativeImage) -> Unit): IThumbnailAccessContract {
        val ref = LayerReference(layer, workspace)
        cache[ref]?.also(onBuilt)

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(layer, workspace,buildLambda(ref,onBuilt)) ).also { contracts[ref] = it }

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    override fun contractThumbnail(part: SpritePart, workspace: IImageWorkspace, onBuilt: (NativeImage) -> Unit): IThumbnailAccessContract {
        val ref = SpritePartReference(part, workspace)
        cache[ref]?.also(onBuilt)

        val set = contracts[ref] ?: ContractSet(rootThumbnailStore.contractThumbnail(part, workspace,buildLambda(ref,onBuilt)) ).also { contracts[ref] = it }

        val contract = ThumbnailAccessContract(ref,onBuilt)
        set.externalContracts.add(WeakReference(contract))
        return contract
    }

    private fun buildLambda( ref: ReferenceObject, onBuilt: (NativeImage) -> Unit) : (IImage)->Unit {
        return {img ->
            val contractSet = contracts[ref]
            if( contractSet != null)
            {
                contractSet.externalContracts.removeIf {it.get() == null }

                if( !contractSet.externalContracts.any()) {
                    contracts.remove(ref)
                    contractSet.internalContract.release()
                }
                else {
                    val native = Hybrid.imageConverter.convert<NativeImage>(img)
                    cache[ref] = native
                    contractSet.externalContracts.forEach {
                        val contract = it.get()
                        contract?.also { it.onBuilt.invoke(native)}
                    }
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

class ThumbnailStore(
        settings: ISettingsManager,
        centralObservatory: ICentralObservatory,
        private val workspaceSet: IWorkspaceSet) : IThumbnailStore<IImage>
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
            return existing
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
        try {
            contracts.removeIf {
                val contract = it.get()
                when (contract) {
                    null -> true
                    else -> {
                        val thumbnail = thumbnailCache[contract.ref]
                        if (thumbnail == null || (thumbnail.changed && now - thumbnail.made > lifespan)) {
                            val updatedThumbnail = createOrUpdateThumbnail(contract.ref)
                            contract.onBuilt.invoke(updatedThumbnail.image)
                        }
                        false
                    }
                }
            }
        }catch (e: Exception) {
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
            else -> existing.also { it.made = Hybrid.timing.currentMilli; it.changed = false }
        }

        val gc= thumbnail.image.graphics
        gc.clear()
        source.render(RenderSettings(32, 32, false), gc)

        return thumbnail
    }

    init {
        Hybrid.timing.createTimer(1000, true) {
            Hybrid.gle.runInGLContext {
                removeUnused()
                cycleContracts()
            }
        }
    }
    // endregion

    init {
    }
    val x =object : ImageObserver {
        override fun imageChanged(evt: ImageChangeEvent) {
            val workspace = workspaceSet.currentWorkspace ?: return
            val mediums = evt.handlesChanged.toHashSet()
            val nodes = evt.nodesChanged.toHashSet()

            thumbnailCache.forEach { ref, thumbnail ->
                if( workspace == ref.workspace && !thumbnail.changed) {
                    when {
                        (ref is SpritePartReference && mediums.contains(ref.part.handle)) ||
                        (ref is LayerReference && ref.layer.imageDependencies.any { mediums.contains(it) }) ||
                        (ref is NodeReference && (ref.node.descendants.any{nodes.contains(it)} || ref.node.imageDependencies.any { mediums.contains(it) }))
                        ->
                        {
                            thumbnail.changed = true
                        }
                    }
                }
            }
        }
    }.also { centralObservatory.trackingImageObserver.addObserver(it.observer())}
}