package spirite.base.imageData

import spirite.base.brains.ICruddyOldObservable
import spirite.base.brains.CruddyOldObservable
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.groupTree.GroupTree.Node

interface IImageObservatory {
    class ImageChangeEvent(
            val handlesChanged: Collection<MediumHandle>,
            val nodesChanged: Collection<Node>,
            val workspace: IImageWorkspace,
            val liftedChange: Boolean = false) {}

    interface ImageObserver {
        fun imageChanged( evt: ImageChangeEvent)
    }

    val imageObservable : ICruddyOldObservable<ImageObserver>

    fun triggerRefresh( evt: ImageChangeEvent)
}

class ImageObservatory : IImageObservatory {
    override val imageObservable = CruddyOldObservable<ImageObserver>()

    override fun triggerRefresh(evt: ImageChangeEvent) {
        imageObservable.trigger { it.imageChanged(evt) }
    }
}