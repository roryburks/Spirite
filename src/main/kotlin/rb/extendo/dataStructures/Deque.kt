package rb.extendo.dataStructures


class Deque<T> : Iterable<T> {
    private var frontNode: Node<T>? = null
    private var backNode: Node<T>? = null

    private class Node<T>(val t: T, var previous: Node<T>?, var next: Node<T>?)
    var length: Int = 0 ; private set


    fun addBack(t: T) {
        ++length
        val prevBackNode = backNode
        val newNode = Node(t, prevBackNode, null)
        if (prevBackNode == null) {
            frontNode = newNode
        } else {
            prevBackNode.next = newNode
        }
        backNode = newNode
    }

    fun addFront(t: T) {
        ++length
        val prevFrontNode = frontNode
        val newNode = Node(t, null, prevFrontNode)
        if (prevFrontNode == null) {
            backNode = newNode
        } else {
            prevFrontNode.previous = newNode
        }
        frontNode = newNode
    }

    fun peekFront() = frontNode?.t
    fun peekBack() = backNode?.t

    fun popFront(): T? {
        val previousFront = frontNode
        return when (previousFront) {
            null -> null
            backNode -> {
                --length
                frontNode = null
                backNode = null
                previousFront.t
            }
            else -> {
                --length
                frontNode = previousFront.next
                frontNode?.previous = null
                previousFront.t
            }
        }
    }

    fun popBack(): T? {
        val previousBack = backNode
        return when (previousBack) {
            null -> null
            frontNode -> {
                --length
                frontNode = null
                backNode = null
                previousBack.t
            }
            else -> {
                --length
                backNode = previousBack.previous
                backNode?.next = null
                previousBack.t
            }
        }
    }

    private class DequeueIterator<T>(var node: Node<T>?) : Iterator<T> {
        override fun hasNext() = node != null

        override fun next(): T {
            val currentNode = node
            return when (currentNode) {
                null -> throw IndexOutOfBoundsException("No more")
                else -> {
                    node = currentNode.next
                    currentNode.t
                }
            }
        }
    }

    private class BackwardsDequeueIterator<T>(var node: Node<T>?) : Iterator<T> {
        override fun hasNext() = node != null

        override fun next(): T {
            val currentNode = node
            return when (currentNode) {
                null -> throw IndexOutOfBoundsException("No more")
                else -> {
                    node = currentNode.previous
                    currentNode.t
                }
            }
        }
    }

    override fun iterator(): Iterator<T> = DequeueIterator(frontNode)
    fun backIterator(): Iterator<T> = DequeueIterator(frontNode)
}