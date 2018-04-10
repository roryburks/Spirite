package spirite.base.imageData.selection

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.graphics.IImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.IAnchorLiftModule
import spirite.base.imageData.drawer.IImageDrawer.ILiftSelectionModule
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.util.delegates.DerivedLazy
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.hybrid.Hybrid

interface ISelectionEngine {
    val selectionChangeObserver: IObservable<()->Any?>
    val selection : Selection?
    val selectionTransform: Transform?
    fun setSelection(newSelection: Selection?)
    fun mergeSelection(newSelection: Selection, mode: BuildMode)
    fun transformSelection( transform: Transform, liftIfEmpty: Boolean = false)

    val liftedData: ILiftedData?
    fun anchorLifted()
    fun clearLifted()
    fun attemptLiftData( drawer: IImageDrawer)
    fun imageToSelection( image: IImage, transform: Transform?)

    var proposingTransform: Transform?
    fun applyProposingTranform()


    enum class BuildMode {
        DEFAULT, ADD, SUBTRACT, INTERSECTION
    }
}


class SelectionEngine(
        val workspace: IImageWorkspace
) : ISelectionEngine {

    private val selectionDerived = DerivedLazy { selectionMask?.let { Selection(it, selectionTransform) }}
    override val selection by selectionDerived

    // region Base Selection Stuff
    override var selectionTransform : Transform? = null
        private set(value) {
            field = value
            selectionDerived.reset()
        }
    private var selectionMask : IImage? = null
        set(value) {
            field = value
            selectionDerived.reset()
        }

    override fun setSelection(newSelection: Selection?) {
        val liftedData = liftedData
        if( liftedData == null) {
            workspace.undoEngine.performAndStore(ChangeSelectionAction(selection, newSelection))
        }
        else {
            val drawer = workspace.activeDrawer
            workspace.undoEngine.doAsAggregateAction({
                val proposingTransform = proposingTransform
                val anchorTransform = when( proposingTransform ) {
                    null -> selectionTransform
                    else -> proposingTransform * (selectionTransform ?: Transform.IdentityMatrix)
                }
                (drawer as? IAnchorLiftModule)?.apply { if( acceptsLifted(liftedData)) anchorLifted(liftedData, anchorTransform)}

                workspace.undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = ""
                    override fun performAction() {this@SelectionEngine.liftedData = null}
                    override fun undoAction() {this@SelectionEngine.liftedData = liftedData}
                })

                workspace.undoEngine.performAndStore(ChangeSelectionAction(selection, newSelection))
            }, "Change Selection")
        }
    }

    inner class ChangeSelectionAction(
            val oldSelection: Selection?,
            val newSelection: Selection?)
        : NullAction()
    {
        override val description: String get() = "Change Selection"

        override fun performAction() {
            selectionMask = newSelection?.mask
            selectionTransform = newSelection?.transform
            selectionChangeObserver.trigger { it() }
        }

        override fun undoAction() {
            selectionMask = oldSelection?.mask
            selectionTransform = oldSelection?.transform
            selectionChangeObserver.trigger { it() }
        }
    }

    override fun mergeSelection(newSelection: Selection, mode: BuildMode) {
        when( mode) {
            ISelectionEngine.BuildMode.DEFAULT -> setSelection(newSelection)
            ISelectionEngine.BuildMode.ADD -> setSelection(selection?.let { it + newSelection} ?: newSelection)
            ISelectionEngine.BuildMode.SUBTRACT -> selection?.let { setSelection(it - newSelection) }
            ISelectionEngine.BuildMode.INTERSECTION -> selection?.let { setSelection(it intersection newSelection) }
        }
    }

    override fun transformSelection(transform: Transform, liftIfEmpty: Boolean) {
        if( selectionMask == null) return

        val liftedData = liftedData
        val drawer = workspace.activeDrawer
        if( !liftIfEmpty || liftedData != null || drawer !is ILiftSelectionModule)
            workspace.undoEngine.performAndStore(TransformSelectionAction(transform))
        else {
            TODO()

        }
    }

    inner class TransformSelectionAction(
            var transform: Transform) : NullAction(), StackableAction
    {
        val originalTransform = selectionTransform

        override val description: String get() = "Moved Selection"

        override fun performAction() {
            selectionTransform = transform * (originalTransform ?: Transform.IdentityMatrix)
        }
        override fun undoAction() {
            selectionTransform = originalTransform
        }

        override fun canStack(other: UndoableAction) = other is TransformSelectionAction
        override fun stackNewAction(other: UndoableAction) {
            transform = (other as TransformSelectionAction).transform * transform
        }

    }
    // endregion

    override var liftedData: ILiftedData? = null
        private set
    override fun anchorLifted() {
        val liftedData = liftedData ?: return
        val drawer = workspace.activeDrawer

        if( drawer !is IAnchorLiftModule || drawer.acceptsLifted(liftedData))
            Hybrid.beep()
        else
            drawer.anchorLifted(liftedData, selectionTransform)
    }

    override fun clearLifted() {
        val oldLifted = liftedData
        if( oldLifted != null) {
            workspace.undoEngine.performAndStore( object: NullAction() {
                override val description: String get() = "Clear lifted Data"

                override fun performAction() {
                    liftedData = null
                    selectionChangeObserver.trigger { it() }
                }
                override fun undoAction() {
                    liftedData = oldLifted
                    selectionChangeObserver.trigger { it() }
                }
            })
        }
    }

    override fun attemptLiftData(drawer: IImageDrawer) {
        val selection = selection ?: return
        if( drawer is ILiftSelectionModule) {
            val newLifted = drawer.liftSelection(selection)
            TODO()
        }
        else
            Hybrid.beep()
    }

    override fun imageToSelection(image: IImage, transform: Transform?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var proposingTransform: Transform? = null

    override fun applyProposingTranform() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



    override val selectionChangeObserver = Observable<()->Any?>()


    init {
        // DEBUG
        setSelection(Selection.RectangleSelection(Rect(100,100)))
        liftedData = LiftedImageData(Hybrid.imageCreator.createImage(10,10).apply {graphics.fillRect(0,0,10,10) })
    }

}