package spirite.base.imageData

import rb.owl.IObservable
import rb.owl.Observable
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.groupTree.Node

interface IImageObservatory {
    class ImageChangeEvent(
        val handlesChanged: Collection<MediumHandle>,
        val nodesChanged: Collection<Node>,
        val workspace: IImageWorkspace,
        val liftedChange: Boolean = false) {}

    interface ImageObserver {
        fun imageChanged( evt: ImageChangeEvent)
    }

    val imageObservable : IObservable<ImageObserver>

    fun triggerRefresh( evt: ImageChangeEvent)
}

class ImageObservatory : IImageObservatory {
    override val imageObservable = Observable<ImageObserver>()

    override fun triggerRefresh(evt: ImageChangeEvent) {
        imageObservable.trigger { it.imageChanged(evt) }
    }
}