package spirite.base.imageData.animation.services

import rb.extendo.extensions.atlasOf
import rb.extendo.extensions.removeToList
import rb.vectrix.mathUtil.MathUtil
import spirite.base.imageData.animation.Animation

interface IAnimationStateSvc {
    var numStateDomains : Int
    var currentStateDomain : Int

    fun getState(anim: Animation) : AnimationStateBind
}

class AnimationStateSvc(private val _animManager : IAnimationManagementSvc) : IAnimationStateSvc
{
    private val _stateBindMap = mutableMapOf<Animation, AnimationStateBind>()
    private val _storedStateAtlas = atlasOf<Int,Animation, AnimationStateData>()

    override var numStateDomains: Int = 2
        set(value) {
            // Make sure to maintain numStateDomains at least = 1
            // and make sure that currentStateDomain is never greater than that num - 1
            val value = if( value < 1) 1 else value

            field = value

            // Ordering:  numStateDomainsUpdated -> currentStateDomainUpdated -> triggers based on state domain changing
            if( currentStateDomain > value - 1)
                currentStateDomain = value - 1
        }
    override var currentStateDomain: Int = 0
        set(value) {
            val value = MathUtil.clip(0, value, numStateDomains - 1)
            if( field != value) {
                val old = value
                field = value
                updateStateBinds(old)
            }
        }

    override fun getState(anim: Animation): AnimationStateBind {
        val existing = _stateBindMap[anim]
        if( existing != null)
            return existing

        val new = AnimationStateBind()
        _stateBindMap[anim] = new
        return new
    }

    fun updateStateBinds(oldDomain: Int) {
        // Basic process:
        // 1. Archive the data that's currently in the active bindings into the Archive Atlas for the old domain
        // 2. Pull out the Archived Map from the Atlas for the new Id
        //  (if it doesn't exist, no changes and the new domain is implicitly being cloned from the old)
        val animations = _animManager.animations
        val mapToArchive = animations
            .associateWith { getState(it).toData() }
        _storedStateAtlas[oldDomain] = mapToArchive.toMutableMap()

        val archivedMap = _storedStateAtlas[currentStateDomain]
        if( archivedMap != null) {
            for( anim in animations) {
                val toMigrateState = archivedMap[anim] ?: continue
                val state = getState(anim)
                toMigrateState.duplicateInto(state)
            }
        }
    }
}