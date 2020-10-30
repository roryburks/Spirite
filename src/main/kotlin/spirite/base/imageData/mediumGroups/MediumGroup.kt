package spirite.base.imageData.mediumGroups

import rb.extendo.dataStructures.SinglySet
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.undo.NullAction


class MediumGroup (){
    private val _groupObjects = mutableListOf<MediumGroupObject>()

    data class MediumGroupObject(
            val node: GroupTree.Node,
            val useFullHierarchy: Boolean)


    // region Interactions
    val groupObjects : List<MediumGroupObject> get() = _groupObjects

    fun addObject(obj: MediumGroupObject) = addObjects(SinglySet(obj))
    fun addObjects(obj: Collection<MediumGroupObject>){
        TODO()
    }

    fun removeObject( obj: MediumGroupObject) = removeObjects(SinglySet(obj))
    fun removeObjects( obj: Collection<MediumGroupObject>){ TODO()}
    // endregion

    private inner class MediumGroupMutationAction(
            val oldGroups: List<MediumGroupObject>,
            val newGroups: List<MediumGroupObject>,
            override val description: String) : NullAction()
    {
        override fun performAction() {
            _groupObjects.clear()
            _groupObjects.addAll(newGroups)
        }

        override fun undoAction() {
            _groupObjects.clear()
            _groupObjects.addAll(oldGroups)
        }
    }
}