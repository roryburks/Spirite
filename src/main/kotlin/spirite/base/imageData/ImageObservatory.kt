package spirite.base.imageData

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
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

    val imageObservers : IObservable<ImageObserver>

    fun triggerRefresh( evt: ImageChangeEvent)
}

class ImageObservatory : IImageObservatory {
    override val imageObservers = Observable<ImageObserver>()

    override fun triggerRefresh(evt: ImageChangeEvent) {
        imageObservers.trigger { it.imageChanged(evt) }
    }

}