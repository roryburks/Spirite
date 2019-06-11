package rb.owl.bindableMList

fun <T> IBindableMList<T>.onRemove(lambda: (removed: Collection<T>)->Unit) = addObserver(
            object : IListTriggers<T> {
                override fun elementsAdded(index: Int, elements: Collection<T>) {}
                override fun elementsChanged(changes: Set<ListChange<T>>) {}
                override fun elementsPermuted(permutation: ListPermuation) {}

                override fun elementsRemoved(elements: Collection<T>) = lambda(elements)
            }.observer()
        )