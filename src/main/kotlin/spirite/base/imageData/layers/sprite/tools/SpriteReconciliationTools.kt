//package spirite.base.imageData.layers.sprite.tools
//
//import rb.extendo.dataStructures.Deque
//import rb.extendo.dataStructures.SinglyList
//import rb.global.SuccessResponse
//import spirite.base.imageData.layers.sprite.SpriteLayer
//
//
//
//object SpriteReconciliationTools {
//    class SpriteReconciliationProposal(
//        val warnings: List<String>?,
//        val errors: List<String>?,
//        val proposal: Map<String, Int>)
//    {
//        companion object {
//            fun Error( error: String) = SpriteReconciliationProposal(null, listOf(error), mapOf())
//        }
//    }
//
//    /**
//     * Given a list of Sprites, tries to reconcile it such that:
//     *  - All overlapping part names have the same depth
//     *  - Within any given sprite, relative ordering is preserved (this might not be possible)
//     *
//     * sprites: List of sprites in order of priority.
//     * override: if true, will run the command to completion even if Warnings Pop Up
//     */
//    fun constructReconciliationProposal(sprites: List<SpriteLayer>, override: Boolean = false) : SpriteReconciliationProposal {
//        val warnings = mutableListOf<String>()
//        if (!sprites.any())
//            return SpriteReconciliationProposal.Error( "No Sprites Selected")
//
//        val allPartNames = sprites
//            .flatMap { it.parts }
//            .map { it.partName }
//            .toSet()
//
//        // Go through the Sprites and Build the Tree
//        val deferredTree = ReconTree()
//        sprites.forEachIndexed { priority, sprite ->
//            val parts = sprite.parts
//            for( i in 1..parts.lastIndex) {
//                val left = parts[i-1].partName
//                val right = parts[i].partName
//                val existingLeft = deferredTree.getNode(left)
//                val existingRight = deferredTree.getNode(right)
//
//                val leftNode = existingLeft ?:
//                    deferredTree.Node(priority, null, left)
//
//                if( existingRight == null){
//                    val existingIndex = leftNode.nameList.indexOf(left)
//
//                    // Accumulation
//                    if( existingIndex == leftNode.nameList.lastIndex)
//                        leftNode.addToEnd(right)
//
//                    // Parented Chain Creation
//                    else
//                        deferredTree.Node(priority, left, right)
//                }
//                else {
//                    // Starting with the right, search left for the left
//                    val rightIndex = existingRight.nameList.indexOf(right)
//                    val leftIsAncestorOfRight = existingRight.leftSearch(left, rightIndex) != null
//
//                    // If Left is Ancestor of Right, the existing is compatible and continue
//                    if( leftIsAncestorOfRight)
//                        continue
//
//                    val leftIndex = leftNode.nameList.indexOf(left)
//                    val rightIsAncestorOfLeft = leftNode.leftSearch(right, leftIndex) != null
//                    if( rightIsAncestorOfLeft) {
//                        warnings.add("Incompatible relationship: $left : $right")
//                        continue
//                    }
//
//                    // No existing ancestry between them, so make one
//                    if( existingRight.parent == null){
//                        // Case 1: Split both nodes and have the right side of the right node have an ancestor
//                        // relationship with the left side of the left
//
//                        // Split Right
//                        if( rightIndex == 0) {
//                            // No splitting needed at index 0
//                            existingRight.parent = left
//                        }
//                        else {
//
//                        }
//                    }
//                    else {
//
//                    }
//                }
//            }
//        }
//
//
//        return SpriteReconciliationProposal(warnings, null, mapOf())
//    }
//    private class ReconTree {
//        private val _asList = mutableListOf<Node>()
//        private val _asMap = mutableMapOf<String,Node>()
//
//        fun getNode(name : String) = _asMap[name]
//
//        inner class Node( val priority : Int, var parent: String?, first: String)
//        {
//            val nameList: List<String> get() = _nameList
//            private val _nameList = mutableListOf(first)
//
//            init {
//                _asList.add(this)
//                _asMap[first] = this
//            }
//
//            fun addToEnd(name: String) {
//                _nameList.add(name)
//                _asMap[name] = this
//            }
//
//            fun splitOn(index: Int) : Node {
//                val list = mutableListOf<String>()
//                while( _nameList.lastIndex >= index)
//                    list.add(_nameList.removeAt(index))
//
//                val node = Node(priority, null, )
//
//
//            }
//
//            fun leftSearch(name: String, startIndex: Int) : Pair<Node,Int>? {
//                var caretIndex = startIndex
//                var caretNode = this
//                while( true) {
//                    if( --caretIndex < 0)  {
//                        val caretParent = caretNode.parent ?: return null
//                        caretNode = _asMap[caretParent] ?: return null
//                        caretIndex = caretNode.nameList.indexOf(caretParent)
//                    }
//                    if( caretNode.nameList[caretIndex] == name)
//                        return Pair(caretNode, caretIndex)
//                }
//            }
//
//            fun rightSearch( name: String, startIndex: Int) : Pair<Node,Int>? {
//                val recQueue = Deque.From(Pair(this, startIndex))
//                while(recQueue.peekFront() != null ){
//                    val (caretNode, si) = recQueue.popFront() ?: return null
//                    for( i in si..caretNode.nameList.lastIndex) {
//                        val caretName = caretNode.nameList[i]
//                        if( caretNode.nameList[i] == name)
//                            return Pair(caretNode, i)
//
//                        _asList
//                            .filter { it.parent == caretName }
//                            .forEach { recQueue.addBack(Pair(it, 0)) }
//                    }
//                }
//                return null
//            }
//        }
//    }
//
//}