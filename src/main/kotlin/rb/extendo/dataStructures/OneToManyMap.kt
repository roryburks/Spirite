package rb.extendo.dataStructures


/**
 * NOTE: Not Concurrent-safe
 */
class MutableOneToManyMap<ONE,MANY>
{
    val one_to_many = mutableMapOf<ONE,MutableList<MANY>>()
    val many_to_one = mutableMapOf<MANY,ONE>()

    fun getOne( any: MANY) = many_to_one[any]
    fun getMany( one: ONE) = one_to_many[one]

    fun dissociate( any: MANY) {
        val one = many_to_one[any]
        if( one != null) {
            many_to_one.remove(any)
        }

        val many = one_to_many[one]
        if( many != null) {
            many.remove( any)
            if( many.isEmpty())
                one_to_many.remove(one)
        }
    }

    fun assosciate( any: MANY, one: ONE) {
        dissociate(any)

        many_to_one.put( any, one)

        val many = one_to_many[one]
        if( many == null) {
            one_to_many.put(one, mutableListOf(any))
        }
        else
            many.add(any)
    }
}