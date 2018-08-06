package spirite.base.graphics.rendering

import spirite.base.brains.CentralObservatory
import spirite.base.brains.ICentralObservatory
import spirite.base.brains.IMasterControl
import spirite.base.brains.settings.ISettingsManager
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.sources.GroupNodeSource
import spirite.base.graphics.rendering.sources.LayerSource
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import java.awt.image.BufferedImage

interface IThumbnailStore
{
    fun accessThumbnail( node: Node, workspace: IImageWorkspace) : IImage
}

interface IThumbnailStoreBi
{
    fun accessThumbnail( node: Node, workspace: IImageWorkspace) : ImageBI
}
class ThumbnailStoreBi(settings: ISettingsManager, centralObservatory: ICentralObservatory, private val originalStore: IThumbnailStore): IThumbnailStoreBi
{
    private val useOnlyBi = true
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