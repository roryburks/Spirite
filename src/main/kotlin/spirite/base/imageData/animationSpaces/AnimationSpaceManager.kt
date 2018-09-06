package spirite.base.imageData.animationSpaces

import spirite.base.util.binding.Bindable
import spirite.base.util.binding.IBindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animationSpaces.IAnimationSpaceManager.AnimationSpaceObserver
import spirite.base.imageData.undo.NullAction
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED

interface  IAnimationSpaceManager
{
    val workspace: IImageWorkspace

    var currentAnimationSpace : AnimationSpace?
    val currentAnimationSpaceBind: IBindable<AnimationSpace?>
    val animationSpaces : List<AnimationSpace>

    fun addAnimationSpace(space: AnimationSpace, select: Boolean = false)
    fun removeAnimationSpace(space: AnimationSpace)

    interface AnimationSpaceObserver {
        fun spaceAdded(space: AnimationSpace)
        fun spaceRemoved( space: AnimationSpace)
    }
    val animationSpaceObservable: IObservable<AnimationSpaceObserver>
}

class AnimationSpaceManager(override val workspace: IImageWorkspace) : IAnimationSpaceManager
{

    override val currentAnimationSpaceBind = Bindable<AnimationSpace?>(null)
    override var currentAnimationSpace: AnimationSpace?
            get() = currentAnimationSpaceBind.field
            set(value) {
                if( value == null || animationSpaces.contains(value))
                {
                    currentAnimationSpaceBind.field = value
                }
                else {
                    MDebug.handleWarning(UNSUPPORTED, "Cannot Chose an animation space outside of the AnimationSpaceManager")
                }
            }

    override fun addAnimationSpace(space: AnimationSpace, select: Boolean) {
        if( space.workspace != workspace){
            MDebug.handleWarning(UNSUPPORTED, "Cannot Import AnimationSpace into a different Workspace")
            return
        }

        workspace.undoEngine.performAndStore( object : NullAction() {
            override val description: String get() = "Added New Animation Space ${space.name}"
            override fun performAction() =_add(space)
            override fun undoAction() = _remove(space)
        })
        if( select) {
            currentAnimationSpace = space
        }
    }

    override fun removeAnimationSpace(space: AnimationSpace) {
        workspace.undoEngine.performAndStore( object : NullAction() {
            override val description: String get() = "Removed Animation Space ${space.name}"
            override fun performAction() = _remove(space)
            override fun undoAction() = _add(space)
        })
    }

    private fun _add(space: AnimationSpace)
    {
        _animationSpaces.add(space)
        animationSpaceObservable.trigger { it.spaceAdded(space) }
    }
    private fun _remove(space: AnimationSpace)
    {
        _animationSpaces.add(space)
        if( currentAnimationSpace == space)
            currentAnimationSpace = null
        animationSpaceObservable.trigger { it.spaceRemoved(space) }
    }

    override val animationSpaces: List<AnimationSpace> get() = _animationSpaces
    private val _animationSpaces = mutableListOf<AnimationSpace>()

    override val animationSpaceObservable = Observable<AnimationSpaceObserver>()
}