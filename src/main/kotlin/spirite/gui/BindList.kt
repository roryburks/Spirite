package spirite.gui

class BindList<T> {
    val data get() = underlying.field
    private var underlying = BindListUnderlying(this)

    var onChange : (()->Unit)? = null
    var onAdd : ((T)->Unit)? = null
    var onRemove : ((T)->Unit)? = null

    fun add( value: T) = add( underlying.field.size - 1, value)
    fun add( index: Int, value: T) {
        underlying.field.add(index, value)
        underlying.bindings.forEach {
            onChange?.invoke()
            onAdd?.invoke(value)
        }
    }

    fun remove( toRemove: T) {
        if(underlying.field.remove(toRemove))
            underlying.bindings.forEach {
                onChange?.invoke()
                onAdd?.invoke(toRemove)
            }
    }
    fun removeAt( index: Int) {
        val removed = underlying.field.removeAt(index)
        underlying.bindings.forEach{
            it.onChange?.invoke()
            it.onRemove?.invoke(removed)
        }
    }

    fun clear() {
        val removedList = data.toList()
        underlying.field.clear()
        underlying.bindings.forEach { onChange?.invoke() }
        removedList.forEach { removed -> underlying.bindings.forEach { it.onRemove?.invoke(removed) } }
    }


    private class BindListUnderlying<T>( bind: BindList<T>)
    {
        val field = mutableListOf<T>()

        val bindings = mutableListOf<BindList<T>>()
        fun swallow( other: BindListUnderlying<T>) {
            val thisOnly = field - other.field
            val otherOnly = other.field - field
            other.bindings.forEach { bind ->
                bind.onChange?.invoke()
                thisOnly.forEach { bind.onAdd?.invoke(it) }
                otherOnly.forEach { bind.onRemove?.invoke(it) }
            }

            bindings.addAll(other.bindings)
        }

        fun detatch( toRemove: BindList<T>) {
            bindings.remove(toRemove)
        }

    }
}

