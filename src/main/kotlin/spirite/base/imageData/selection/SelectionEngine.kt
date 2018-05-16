package spirite.base.imageData.selection

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.graphics.IImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.IAnchorLiftModule
import spirite.base.imageData.drawer.IImageDrawer.ILiftSelectionModule
import spirite.base.imageData.selection.ISelectionEngine.BuildMode
import spirite.base.imageData.selection.ISelectionEngine.SelectionChangeEvent
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.util.delegates.DerivedLazy
import spirite.base.util.linear.Transform
import spirite.hybrid.Hybrid

interface ISelectionEngine {
    val selectionChangeObserver: IObservable<(SelectionChangeEvent)->Any?>
    val selection : Selection?
    val selectionTransform: Transform?
    fun setSelection(newSelection: Selection?)
    fun mergeSelection(newSelection: Selection, mode: BuildMode)
    fun transformSelection( transform: Transform, liftIfEmpty: Boolean = false)
    fun bakeTranslationIntoLifted()

    val liftedData: ILiftedData?
    fun anchorLifted()
    fun clearLifted()
    fun attemptLiftData( drawer: IImageDrawer)
    fun setSelectionWithLifted(newSelection: Selection, lifted: ILiftedData)
    fun imageToSelection( image: IImage, transform: Transform?)

    var proposingTransform: Transform?
    fun applyProposingTransform()

    class SelectionChangeEvent(
            val isLiftedChange: Boolean = false
    )

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
        val newSelection = if( newSelection?.empty == true) null else newSelection
        val liftedData = liftedData
        if( liftedData == null) {
            workspace.undoEngine.performAndStore(ChangeSelectionAction(newSelection))
        }
        else {
            val drawer = workspace.activeDrawer
            workspace.undoEngine.doAsAggregateAction("Change Selection") {
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

                workspace.undoEngine.performAndStore(ChangeSelectionAction(newSelection))
            }
        }
    }

    inner class ChangeSelectionAction(val newSelection: Selection?)
        : NullAction()
    {
        val oldSelection: Selection? = selection
        override val description: String get() = "Change Selection"

        override fun performAction() {
            selectionMask = newSelection?.mask
            selectionTransform = newSelection?.transform
            selectionChangeObserver.trigger { it(SelectionChangeEvent()) }
        }

        override fun undoAction() {
            selectionMask = oldSelection?.mask
            selectionTransform = oldSelection?.transform
            selectionChangeObserver.trigger { it(SelectionChangeEvent()) }
        }
    }

    override fun mergeSelection(newSelection: Selection, mode: BuildMode) {
        if( newSelection.empty) return
        when( mode) {
            ISelectionEngine.BuildMode.DEFAULT -> setSelection(newSelection)
            ISelectionEngine.BuildMode.ADD -> setSelection(selection?.let { it + newSelection} ?: newSelection)
            ISelectionEngine.BuildMode.SUBTRACT -> selection?.let { setSelection(it - newSelection) }
            ISelectionEngine.BuildMode.INTERSECTION -> selection?.let { setSelection(it intersection newSelection) }
        }
    }

    override fun transformSelection(transform: Transform, liftIfEmpty: Boolean) {
        val selection = selection ?: return

        val liftedData = liftedData
        val drawer = workspace.activeDrawer
        if( !liftIfEmpty || liftedData != null || drawer !is ILiftSelectionModule)
            workspace.undoEngine.performAndStore(TransformSelectionAction(transform))
        else {
            workspace.undoEngine.doAsAggregateAction("Transform", true){
                liftData(drawer, selection)
                workspace.undoEngine.performAndStore(TransformSelectionAction(transform))
            }
        }
    }

    inner class TransformSelectionAction(
            var transform: Transform) : NullAction(), StackableAction
    {
        val originalTransform = selectionTransform

        override val description: String get() = "Moved Selection"

        override fun performAction() {
            selectionTransform = transform * (originalTransform ?: Transform.IdentityMatrix)
            workspace.activeData?.handle?.refresh()
        }
        override fun undoAction() {
            selectionTransform = originalTransform
            workspace.activeData?.handle?.refresh()
        }

        override fun canStack(other: UndoableAction) = other is TransformSelectionAction
        override fun stackNewAction(other: UndoableAction) {
            transform = (other as TransformSelectionAction).transform * transform
        }

    }

    override fun bakeTranslationIntoLifted() {
        val selection = selection ?: return
        val liftedData = liftedData ?: return
        val selectionMask = selectionMask ?: return

        val pair = liftedData.bake(selection.transform ?: return)

        val newSelection = Selection(selectionMask, pair.second, false)

        workspace.undoEngine.doAsAggregateAction("Bake Lifted") {
            workspace.undoEngine.performAndStore(ChangeSelectionAction(newSelection))
            workspace.undoEngine.performAndStore(ChangeLiftedDataAction(pair.first))
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

    override fun setSelectionWithLifted(newSelection: Selection, lifted: ILiftedData) {
        workspace.undoEngine.doAsAggregateAction("Set Lifted Selection"){
            setSelection(newSelection)
            workspace.undoEngine.performAndStore(ChangeLiftedDataAction(lifted))
        }
    }

    override fun clearLifted() {
        val oldLifted = liftedData
        if( oldLifted != null) {
            workspace.undoEngine.performAndStore( ChangeLiftedDataAction(null))
        }
    }

    override fun attemptLiftData(drawer: IImageDrawer) {
        val selection = selection ?: return
        if( drawer is ILiftSelectionModule)
            liftData(drawer, selection)
        else
            Hybrid.beep()
    }

    private fun liftData( drawer: ILiftSelectionModule, selection: Selection) {
        workspace.undoEngine.doAsAggregateAction("Lifted Data") {
            workspace.undoEngine.performAndStore(ChangeLiftedDataAction(drawer.liftSelection(selection)))
        }
    }

    inner class ChangeLiftedDataAction(val new: ILiftedData?) : NullAction() {
        val old = liftedData
        override val description: String get() = "Changed Lifted Data"

        override fun performAction() {
            liftedData = new
            selectionChangeObserver.trigger { it(SelectionChangeEvent(true)) }
        }

        override fun undoAction() {
            liftedData = old
            selectionChangeObserver.trigger { it(SelectionChangeEvent(true)) }
        }
    }

    override fun imageToSelection(image: IImage, transform: Transform?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var proposingTransform: Transform? = null

    override fun applyProposingTransform() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



    override val selectionChangeObserver = Observable<(SelectionChangeEvent)->Any?>()

}